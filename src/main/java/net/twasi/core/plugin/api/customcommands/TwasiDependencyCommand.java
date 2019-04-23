package net.twasi.core.plugin.api.customcommands;

import net.twasi.core.database.models.User;
import net.twasi.core.plugin.TwasiDependency;
import net.twasi.core.translations.renderer.TranslationRenderer;

public abstract class TwasiDependencyCommand extends TwasiCustomCommand {

    private TwasiDependency twasiDependency;

    protected TwasiDependencyCommand(TwasiDependency twasiDependency) {
        this.twasiDependency = twasiDependency;
    }

    @Override
    public boolean allowsTimer() {
        return false;
    }

    @Override
    public boolean allowsListing() {
        return false;
    }

    @Deprecated
    protected final String getTranslation(User user, String key, Object... objects) {
        return this.twasiDependency.getTranslation(user, key, objects);
    }

    @Deprecated
    protected final String getRandomTranslation(User user, String key, Object... objects) {
        return this.twasiDependency.getRandomTranslation(user, key, objects);
    }

    @Override
    protected TranslationRenderer getTranslationRenderer() {
        return TranslationRenderer.getInstance(twasiDependency);
    }
}
