package net.twasi.core.graphql.model.customthemes;

import net.twasi.core.database.models.User;
import net.twasi.core.database.repositories.CustomThemeRepository;
import net.twasi.core.database.repositories.UserRepository;
import net.twasi.core.graphql.model.PanelResultDTO;
import net.twasi.core.services.providers.DataService;

import java.util.ArrayList;
import java.util.List;

import static net.twasi.core.graphql.model.PanelResultDTO.PanelResultType.OK;
import static net.twasi.core.graphql.model.PanelResultDTO.PanelResultType.WARNING;

public class CustomThemesDTO {

    private User user;
    private UserRepository userRepo;
    private CustomThemeRepository repo;

    public CustomThemesDTO(User user) {
        this.user = user;
        this.userRepo = DataService.get().get(UserRepository.class);
        this.repo = DataService.get().get(CustomThemeRepository.class);
    }

    public CustomThemePagination getMyThemes(int page) {
        return new CustomThemePagination(
                repo.countThemesByUser(user),
                page,
                repo.getThemesByUser(user, page)
        );
    }

    public List<StoreCustomThemeDTO> getInstalledThemes() {
        try {
            return repo.getInstalledThemesByUser(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public CustomThemePagination getAvailableThemes(int page, boolean approvedOnly) {
        return new CustomThemePagination(
                repo.countAvailableThemes(approvedOnly),
                page,
                repo.getAvailableThemes(page, user, approvedOnly)
        );
    }

    public PanelResultDTO uninstall(String themeId) {
        List<String> installedThemes = new ArrayList<>(user.getInstalledThemes());
        installedThemes.remove(themeId);
        user.setInstalledThemes(installedThemes);
        userRepo.commit(user);
        return new PanelResultDTO(OK);
    }

    public PanelResultDTO install(String themeId) {
        List<String> installedThemes = new ArrayList<>(user.getInstalledThemes());
        if(installedThemes.contains(themeId)) {
            return new PanelResultDTO(WARNING, "CUSTOM-THEMES.ALREADY-INSTALLED");
        }
        installedThemes.add(themeId);
        user.setInstalledThemes(installedThemes);
        userRepo.commit(user);
        return new PanelResultDTO(OK);
    }

    public PanelResultDTO delete(String themeId) {
        if (repo.delete(themeId, user) > 0) {
            return new PanelResultDTO(OK);
        } else {
            return new PanelResultDTO(WARNING, "CUSTOM-THEMES.NO-THEME-FOUND");
        }
    }

    public PanelResultDTO create(String name, CustomThemeDTO properties) {
        repo.create(properties, user, name);
        return new PanelResultDTO(OK);
    }
}
