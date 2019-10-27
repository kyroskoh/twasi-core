package net.twasi.core.database.repositories;

import net.twasi.core.database.lib.Repository;
import net.twasi.core.database.models.CustomTheme;
import net.twasi.core.database.models.User;
import net.twasi.core.graphql.model.customthemes.CustomThemeDTO;
import net.twasi.core.graphql.model.customthemes.StoreCustomThemeDTO;

import java.util.List;
import java.util.stream.Collectors;

public class CustomThemeRepository extends Repository<CustomTheme> {

    private List<StoreCustomThemeDTO> map(List<CustomTheme> list, User queryingUser) {
        return list.stream().map(t -> new StoreCustomThemeDTO(t, queryingUser)).collect(Collectors.toList());
    }

    public List<StoreCustomThemeDTO> getThemesByUser(User user, int page) {
        return map(query().field("creator").equal(user.getId()).asList(paginated(page)), user);
    }

    public long countThemesByUser(User user) {
        return query().field("creator").equal(user.getId()).count();
    }

    public List<StoreCustomThemeDTO> getAvailableThemes(int page, User user, boolean approvedOnly) {
        if (approvedOnly)
            return map(query().field("approved").equal(true).asList(paginated(page)), user);
        else
            return map(query().asList(paginated(page)), user);
    }

    public long countAvailableThemes(boolean approvedOnly) {
        if (approvedOnly)
            return query().field("approved").equal(true).count();
        else
            return count();
    }

    public List<StoreCustomThemeDTO> getInstalledThemesByUser(User user) {
        return map(query().field("id").in(user.getInstalledThemes()).asList(), user);
    }

    public StoreCustomThemeDTO create(CustomThemeDTO properties, User user, String name) {
        CustomTheme theme = new CustomTheme(user.getId(), name, properties);
        add(theme);
        return new StoreCustomThemeDTO(theme, user);
    }

    public int delete(String id, User user) {
        return store.delete(query().field("creator").equal(user).field("id").equal(id)).getN();
    }
}
