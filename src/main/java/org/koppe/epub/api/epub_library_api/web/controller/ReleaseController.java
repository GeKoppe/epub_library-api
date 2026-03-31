package org.koppe.epub.api.epub_library_api.web.controller;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ReleaseController {

    private final String owner = "GeKoppe";
    private String repo = "epub_library-client";

    @GetMapping("/releases")
    public String releases(Model model) {

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        RestClient client = builder.build();

        List<Map<String, Object>> releases = client.get()
                .uri("/repos/{owner}/{repo}/releases", owner, repo)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (releases == null || releases.isEmpty()) {
            model.addAttribute("error", "Keine Releases gefunden.");
            return "releases";
        }

        // Neuestes Release = erstes Element (GitHub sortiert absteigend nach Datum)
        Map<String, Object> release = releases.get(0);

        String tagName     = (String) release.get("tag_name");
        String releaseName = (String) release.getOrDefault("name", tagName);
        String htmlUrl     = (String) release.get("html_url");
        boolean prerelease = Boolean.TRUE.equals(release.get("prerelease"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assets =
                (List<Map<String, Object>>) release.getOrDefault("assets", List.of());

        // Source-Code ZIP & TAR direkt von GitHub generiert
        String zipUrl = "https://github.com/" + owner + "/" + repo + "/archive/refs/tags/" + tagName + ".zip";
        String tarUrl = "https://github.com/" + owner + "/" + repo + "/archive/refs/tags/" + tagName + ".tar.gz";

        // Maven-Artefakte aus den Release-Assets heraussuchen
        Optional<String> jarUrl     = findAsset(assets, ".jar", "-sources", "-javadoc");
        Optional<String> sourcesUrl = findAsset(assets, "-sources.jar");
        Optional<String> javadocUrl = findAsset(assets, "-javadoc.jar");

        model.addAttribute("tagName",     tagName);
        model.addAttribute("releaseName", releaseName);
        model.addAttribute("htmlUrl",     htmlUrl);
        model.addAttribute("prerelease",  prerelease);
        model.addAttribute("zipUrl",      zipUrl);
        model.addAttribute("tarUrl",      tarUrl);
        model.addAttribute("jarUrl",      jarUrl.orElse(null));
        model.addAttribute("sourcesUrl",  sourcesUrl.orElse(null));
        model.addAttribute("javadocUrl",  javadocUrl.orElse(null));

        return "releases";
    }

    /** Findet ein Asset, dessen Name auf suffix endet und keines der excludes enthält. */
    private Optional<String> findAsset(List<Map<String, Object>> assets,
                                        String suffix,
                                        String... excludes) {
        return assets.stream()
                .filter(a -> {
                    String name = (String) a.get("name");
                    if (name == null || !name.endsWith(suffix)) return false;
                    for (String ex : excludes) {
                        if (name.contains(ex)) return false;
                    }
                    return true;
                })
                .map(a -> (String) a.get("browser_download_url"))
                .findFirst();
    }
}
