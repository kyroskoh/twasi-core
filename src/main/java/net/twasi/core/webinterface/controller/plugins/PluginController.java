package net.twasi.core.webinterface.controller.plugins;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.twasi.core.database.Database;
import net.twasi.core.database.models.User;
import net.twasi.core.interfaces.api.TwasiInterface;
import net.twasi.core.plugin.TwasiPlugin;
import net.twasi.core.services.InstanceManagerService;
import net.twasi.core.services.PluginManagerService;
import net.twasi.core.webinterface.dto.ApiDTO;
import net.twasi.core.webinterface.dto.SuccessDTO;
import net.twasi.core.webinterface.dto.error.BadRequestDTO;
import net.twasi.core.webinterface.dto.error.UnauthorizedDTO;
import net.twasi.core.webinterface.lib.Commons;
import net.twasi.core.webinterface.lib.RequestHandler;

import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class PluginController extends RequestHandler {

    @Override
    public void handleGet(HttpExchange t) {
        if (!isAuthenticated(t)) {
            Commons.writeDTO(t, new UnauthorizedDTO(), 401);
            return;
        }

        List<TwasiPlugin> plugins = PluginManagerService.getService().getPlugins();

        PluginListDTO dto = new PluginListDTO(plugins);

        Commons.writeDTO(t, dto, 200);
    }

    @Override
    public void handlePost(HttpExchange t) {
        if (!isAuthenticated(t)) {
            Commons.writeDTO(t, new UnauthorizedDTO(), 401);
            return;
        }
        User user = getUser(t);

        PluginInstallDTO dto = new Gson().fromJson(new InputStreamReader(t.getRequestBody()), PluginInstallDTO.class);

        if (user.getInstalledPlugins().contains(dto.pluginName)) {
            Commons.writeDTO(t, new BadRequestDTO("Plugin is already installed."), 400);
            return;
        }

        if (PluginManagerService.getService().getPlugins().stream().noneMatch(twasiPlugin -> twasiPlugin.getName().equalsIgnoreCase(dto.pluginName))) {
            Commons.writeDTO(t, new BadRequestDTO("This plugin is not available on this instance."), 400);
            return;
        }

        TwasiInterface inf = InstanceManagerService.getService().getByUser(user);
        inf.installPlugin(PluginManagerService.getService().getPlugins().stream().filter(twasiPlugin -> twasiPlugin.getName().equalsIgnoreCase(dto.pluginName)).findFirst().get());

        user.getInstalledPlugins().add(dto.pluginName);
        Database.getStore().save(user);

        Commons.writeDTO(t, new SuccessDTO("Plugin installed"), 200);
    }

    class PluginInstallDTO {
        String pluginName;
    }

    class PluginListDTO extends ApiDTO {
        List<SinglePluginDTO> plugins;

        PluginListDTO(List<TwasiPlugin> plugins) {
            super(true);
            this.plugins = plugins.stream().map(SinglePluginDTO::new).collect(Collectors.toList());
        }

        class SinglePluginDTO {
            String name;
            String author;
            String description;
            String version;

            SinglePluginDTO(TwasiPlugin plugin) {
                name = plugin.getDescription().name;
                author = plugin.getDescription().author;
                description = plugin.getDescription().description;
                version = plugin.getDescription().version;
            }
        }
    }

}
