package me.viciscat.datapack_helper.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import oshi.util.tuples.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatapackEditorScreen extends Screen {

    private static String TITLE = "DATAPACK EDITOR";

    private IntegratedServer integratedServer;

    private int frame = 0;
    public DatapackEditorScreen() {
        super(Text.of(TITLE));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, String.valueOf(frame/24), frame/24, 0, 16777215, true);
        frame += 1;
    }

    protected void init() {
        Objects.requireNonNull(this.textRenderer);
        this.addDrawableChild(new TextWidget(0, 10, this.width, 9, this.title, this.textRenderer));

        integratedServer = MinecraftClient.getInstance().getServer();
        Objects.requireNonNull(this.integratedServer);
        Path savePath = integratedServer.getSavePath(WorldSavePath.DATAPACKS);
        String folderPath = savePath.toString();
        TextWidget pathWidget = new TextWidget((int)(this.width * 0.05), 50, this.textRenderer.getWidth(folderPath), 9, Text.of(folderPath), this.textRenderer);
        this.addDrawableChild(pathWidget);

        int y = 20;
        for (ResourcePackProfile profile : integratedServer.getDataPackManager().getProfiles()) {
            if (profile.getSource() != ResourcePackSource.SERVER && profile.getSource() != ResourcePackSource.WORLD) continue;

            Text text = Text.of(profile.getName());
            TextWidget widget = new TextWidget((int)(this.width * 0.05), y, this.textRenderer.getWidth(text), 9, text, this.textRenderer);
            widget.setTooltip(Tooltip.of(profile.getInformationText(true)));
            this.addDrawableChild(widget);
            y+=10;
        }
        MultilineTextEditor multilineTextEditor = new MultilineTextEditor(this.textRenderer, (int)(this.width * 0.2), 60, 200, 200, Text.of("BALLS"));
        String packMetaString;
        try {
            packMetaString = Files.readString(Path.of(folderPath, "template gaming WOWI/pack.mcmeta"));
        } catch (IOException e) {
            packMetaString = "ERROR";
        }
        multilineTextEditor.setText(packMetaString.replaceAll("\\r", "").replaceAll("§", "&"));
        this.addDrawableChild(multilineTextEditor);

    }
}
