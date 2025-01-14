package com.turki.bannedmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.awt.Color;
import java.io.IOException;

@Mod(modid = "bannedmod", version = "1.0", clientSideOnly = true)
public class bannedMod {
    private static KeyBinding guiKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
	    guiKey = new KeyBinding("key.gui", Keyboard.KEY_B, "BlocksMC Banned GUI");
		ClientRegistry.registerKeyBinding(guiKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

      @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        // Check if our key binding was pressed
        if (guiKey.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new PlayerSelectGui());
        }
    }

    // Shared UI constants and helper methods
    public static class UIHelper {
        public static final Color DARK_BG = new Color(18, 18, 18);
        public static final Color DARK_CARD = new Color(30, 30, 30);
        public static final Color ACCENT = new Color(88, 101, 242);
        public static final Color HOVER = new Color(45, 45, 45);
        public static final int AVATAR_SIZE = 32;
        
        private static final ResourceLocation CLICK_SOUND = new ResourceLocation("random.click");

        public static void playClickSound() {
            Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.create(CLICK_SOUND, 1.0F)
            );
        }

        public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
            // Center
            GuiScreen.drawRect(x + radius, y, x + width - radius, y + height, color);
            
            // Sides
            GuiScreen.drawRect(x, y + radius, x + radius, y + height - radius, color);
            GuiScreen.drawRect(x + width - radius, y + radius, x + width, y + height - radius, color);
            
            // Corners
            drawFilledCircle(x + radius, y + radius, radius, color);
            drawFilledCircle(x + width - radius, y + radius, radius, color);
            drawFilledCircle(x + radius, y + height - radius, radius, color);
            drawFilledCircle(x + width - radius, y + height - radius, radius, color);
        }

        public static void drawFilledCircle(int centerX, int centerY, int radius, int color) {
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            
            float alpha = (color >> 24 & 255) / 255.0F;
            float red = (color >> 16 & 255) / 255.0F;
            float green = (color >> 8 & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;
            
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            
            worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos(centerX, centerY, 0).color(red, green, blue, alpha).endVertex();
            
            int segments = Math.max(radius * 4, 360);
            for (int i = 0; i <= segments; i++) {
                double angle = Math.PI * 2 * i / segments;
                double x = centerX + Math.sin(angle) * radius;
                double y = centerY + Math.cos(angle) * radius;
                worldrenderer.pos(x, y, 0).color(red, green, blue, alpha).endVertex();
            }
            
            tessellator.draw();
            
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
        }
        public static void drawRect(int left, int top, int right, int bottom, int color) {
            GuiScreen.drawRect(left, top, right, bottom, color);
        }

        public static void drawGlowingBorder(int x, int y, int width, int height, int color, float intensity) {
            int glowSize = 2; // Reduced from 4
            float alphaStep = 0.3f; // Reduced alpha intensity
            
            for (int i = 0; i < glowSize; i++) {
                float alphaFactor = (glowSize - i) / (float)glowSize * alphaStep;
                int alpha = (int)(((color >> 24) & 0xFF) * intensity * alphaFactor);
                int borderColor = (color & 0x00FFFFFF) | (alpha << 24);
                
                // Draw glow with reduced size
                drawRect(x - i, y - i, x + width + i, y, borderColor); // Top
                drawRect(x - i, y + height, x + width + i, y + height + i, borderColor); // Bottom
                drawRect(x - i, y, x, y + height, borderColor); // Left
                drawRect(x + width, y, x + width + i, y + height, borderColor); // Right
            }
        }

        public static void drawPlayerHead(EntityPlayer player, int x, int y) {
            NetworkPlayerInfo playerInfo = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(player.getUniqueID());
            ResourceLocation skin = playerInfo != null ? playerInfo.getLocationSkin() : new ResourceLocation("textures/entity/steve.png");
            
            Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
            
            // Draw shadow
            GuiScreen.drawRect(x + 2, y + 2, x + AVATAR_SIZE + 2, y + AVATAR_SIZE + 2, 0x55000000);
            
            // Draw head
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Draw the head portion of the skin (8x8 area starting at 8,8)
            GuiScreen.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, AVATAR_SIZE, AVATAR_SIZE, 64.0F, 64.0F);
            // Draw the hat layer
            GuiScreen.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8, AVATAR_SIZE, AVATAR_SIZE, 64.0F, 64.0F);
            
            GlStateManager.disableBlend();
        }

        public static void drawModernScrollbar(int x, int y, int width, int height, float progress, float viewableRatio) {
            // Background
            GuiScreen.drawRect(x, y, x + width, y + height, new Color(0, 0, 0, 60).getRGB());
            
            // Thumb
            int thumbHeight = Math.max(40, (int)(height * viewableRatio));
            int thumbY = y + (int)((height - thumbHeight) * progress);
            GuiScreen.drawRect(x, thumbY, x + width, thumbY + thumbHeight, ACCENT.getRGB());
        }
    }

    public static class BasebannedGui extends GuiScreen {
        protected int windowWidth;
        protected int windowHeight;
        protected float scrollProgress = 0.0f;
        protected boolean isDragging = false;
        protected int lastMouseY;
        protected long hoverStartTime = 0;

        protected void handleScroll(int maxScroll) {
            int scroll = Mouse.getEventDWheel();
            if (scroll != 0) {
                scrollProgress -= scroll * 0.5f;
                scrollProgress = MathHelper.clamp_float(scrollProgress, 0, maxScroll);
            }
        }

        @Override
        protected void mouseReleased(int mouseX, int mouseY, int state) {
            isDragging = false;
        }
    }

    public static class PlayerSelectGui extends BasebannedGui {
        private List<EntityPlayer> nearbyPlayers;
        private static final int ENTRY_HEIGHT = 50;
        private static final int ENTRIES_PER_PAGE = 6;
        private int hoveredPlayer = -1;
        
        @Override
        public void initGui() {
            super.initGui();
            nearbyPlayers = getNearbyPlayers();
            windowWidth = 280;
            windowHeight = Math.min(350, 60 + (ENTRIES_PER_PAGE * ENTRY_HEIGHT));
        }

        private List<EntityPlayer> getNearbyPlayers() {
            return mc.theWorld.playerEntities.stream()
                .filter(p -> p != mc.thePlayer)
                .sorted(Comparator.comparingDouble(p -> p.getDistanceToEntity(mc.thePlayer)))
                .collect(Collectors.toList());
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xCC000000);
            
            int startX = (width - windowWidth) / 2;
            int startY = (height - windowHeight) / 2;
            int headerHeight = 50;
            
            // Draw main background
            drawRect(startX, startY, startX + windowWidth, startY + windowHeight, UIHelper.DARK_BG.getRGB());
            
            // Draw header background
            drawRect(startX, startY, startX + windowWidth, startY + headerHeight, UIHelper.DARK_CARD.getRGB());
            drawCenteredString(fontRendererObj, "Select Player", startX + windowWidth/2, startY + 20, UIHelper.ACCENT.getRGB());
            
            // Player list with proper clipping
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            double scaleFactor = mc.displayHeight / (double)height;
            GL11.glScissor(
                (int)(startX * scaleFactor),
                (int)(mc.displayHeight - (startY + windowHeight) * scaleFactor),
                (int)(windowWidth * scaleFactor),
                (int)((windowHeight - headerHeight) * scaleFactor)
            );

            // Adjust starting Y position to be below header
            int contentY = startY + headerHeight + 5; // Added 5px padding
            int maxScroll = Math.max(0, nearbyPlayers.size() * ENTRY_HEIGHT - (windowHeight - headerHeight - 10));
            scrollProgress = MathHelper.clamp_float(scrollProgress, 0, maxScroll);

            for (int i = 0; i < nearbyPlayers.size(); i++) {
                EntityPlayer player = nearbyPlayers.get(i);
                int entryY = contentY + (i * ENTRY_HEIGHT) - (int)scrollProgress;
                
                // Only render visible entries and ensure they don't overlap with header
                if (entryY + ENTRY_HEIGHT >= startY + headerHeight && entryY <= startY + windowHeight) {
                    boolean isHovered = mouseX >= startX + 10 && mouseX <= startX + windowWidth - 24 &&
                                      mouseY >= entryY && mouseY < entryY + 40;
                    
                    if (isHovered && hoveredPlayer != i) {
                        hoveredPlayer = i;
                        hoverStartTime = System.currentTimeMillis();
                    }
                    
                    drawPlayerCard(player, startX + 10, entryY, isHovered);
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            // Scrollbar
            if (nearbyPlayers.size() > ENTRIES_PER_PAGE) {
                float viewableRatio = (windowHeight - headerHeight - 10) / 
                                    (float)(nearbyPlayers.size() * ENTRY_HEIGHT);
                UIHelper.drawModernScrollbar(
                    startX + windowWidth - 4, startY + headerHeight,
                    4, windowHeight - headerHeight - 10,
                    scrollProgress / maxScroll,
                    viewableRatio
                );
            }
        }

        private void drawPlayerCard(EntityPlayer player, int x, int y, boolean isHovered) {
            int cardHeight = 40;
            int cardWidth = windowWidth - 44; // Reduced width to prevent overflow
            
            int bgColor = isHovered ? UIHelper.HOVER.getRGB() : UIHelper.DARK_CARD.getRGB();
            UIHelper.drawRoundedRect(x, y, cardWidth, cardHeight, 6, bgColor);
            
            if (isHovered) {
                float glowIntensity = Math.min(1.0f, (System.currentTimeMillis() - hoverStartTime) / 200.0f);
                // Reduce glow border size and contain it within card bounds
                UIHelper.drawGlowingBorder(x, y, cardWidth, cardHeight, UIHelper.ACCENT.getRGB(), glowIntensity);
            }

            UIHelper.drawPlayerHead(player, x + 8, y + (cardHeight - UIHelper.AVATAR_SIZE)/2);
            
            int textX = x + UIHelper.AVATAR_SIZE + 16;
            int textY = y + (cardHeight - 20)/2;
            
            fontRendererObj.drawStringWithShadow(
                player.getName(),
                textX,
                textY,
                isHovered ? UIHelper.ACCENT.getRGB() : 0xFFFFFF
            );
            
            String distance = String.format("%.0f blocks away", player.getDistanceToEntity(mc.thePlayer));
            fontRendererObj.drawStringWithShadow(
                distance,
                textX,
                textY + 12,
                0x888888
            );
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
            handleScroll(Math.max(0, nearbyPlayers.size() * ENTRY_HEIGHT - (windowHeight - 60)));
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        	   int startX = (width - windowWidth) / 2;
        	    int startY = (height - windowHeight) / 2;
        	    
        	    for (int i = 0; i < nearbyPlayers.size(); i++) {
        	        int entryY = startY + 50 + (i * ENTRY_HEIGHT) - (int)scrollProgress;
        	        
        	        // Use the same card height as in drawPlayerCard
        	        int cardHeight = 40;
        	        if (mouseX >= startX + 10 && mouseX <= startX + windowWidth - 24 &&
        	            mouseY >= entryY && mouseY < entryY + cardHeight) { // Update hitbox height
        	            
        	            UIHelper.playClickSound();
        	            mc.displayGuiScreen(new bannedTypeGui(nearbyPlayers.get(i)));
        	            return;
        	        }
        	    }
            
            if (mouseX >= width/2 + windowWidth/2 - 4 && 
                mouseX <= width/2 + windowWidth/2 &&
                mouseY >= startY + 50 && 
                mouseY <= startY + windowHeight - 10) {
                isDragging = true;
                lastMouseY = mouseY;
            }
        }

        @Override
        protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
            if (isDragging) {
                int diff = mouseY - lastMouseY;
                scrollProgress += diff * 2.5f;
                int maxScroll = Math.max(0, nearbyPlayers.size() * ENTRY_HEIGHT - (windowHeight - 60));
                scrollProgress = MathHelper.clamp_float(scrollProgress, 0, maxScroll);
                lastMouseY = mouseY;
            }
        }
    }

    public static class bannedTypeGui extends BasebannedGui {
        private final EntityPlayer targetPlayer;
        private final String[] options = {"Hacking", "Teaming"};
        private static final int OPTION_HEIGHT = 50;
        private int hoveredOption = -1;

        public bannedTypeGui(EntityPlayer player) {
            this.targetPlayer = player;
            this.windowWidth = 250;
            this.windowHeight = 240;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xCC000000);
            
            int startX = (width - windowWidth) / 2;
            int startY = (height - windowHeight) / 2;
            
            // Main window
            drawRect(startX - 1, startY - 1, startX + windowWidth + 1, startY + windowHeight + 1, 0x50000000);
            drawRect(startX, startY, startX + windowWidth, startY + windowHeight, UIHelper.DARK_BG.getRGB());
            
            // Title area
            drawRect(startX, startY, startX + windowWidth, startY + 40, UIHelper.DARK_CARD.getRGB());
            drawCenteredString(fontRendererObj, "Banned Type", startX + windowWidth/2, startY + 15, UIHelper.ACCENT.getRGB());
            
            // Player info card
            int playerCardY = startY + 50;
            UIHelper.drawRoundedRect(startX + 10, playerCardY, windowWidth - 20, 40, 6, UIHelper.DARK_CARD.getRGB());
            UIHelper.drawPlayerHead(targetPlayer, startX + 20, playerCardY + 4);
            fontRendererObj.drawStringWithShadow(targetPlayer.getName(), startX + 60, playerCardY + 15, 0xFFFFFF);
            
            // Options
            int optionsStartY = playerCardY + 50;
            for (int i = 0; i < options.length; i++) {
                boolean isHovered = mouseX >= startX + 10 && mouseX <= startX + windowWidth - 10 &&
                                  mouseY >= optionsStartY + (i * (OPTION_HEIGHT + 8)) && 
                                  mouseY < optionsStartY + (i * (OPTION_HEIGHT + 8)) + OPTION_HEIGHT;
                
                if (isHovered && hoveredOption != i) {
                    hoveredOption = i;
                    hoverStartTime = System.currentTimeMillis();
                }
                
                drawOptionCard(startX + 10, optionsStartY + (i * (OPTION_HEIGHT + 8)), windowWidth - 20, options[i], isHovered);
            }
        }

        private void drawOptionCard(int x, int y, int width, String text, boolean hovered) {
            int bgColor = hovered ? UIHelper.HOVER.getRGB() : UIHelper.DARK_CARD.getRGB();
            UIHelper.drawRoundedRect(x, y, width, OPTION_HEIGHT, 6, bgColor);
            
            if (hovered) {
                float glowIntensity = Math.min(1.0f, (System.currentTimeMillis() - hoverStartTime) / 200.0f);
                UIHelper.drawGlowingBorder(x, y, width, OPTION_HEIGHT, UIHelper.ACCENT.getRGB(), glowIntensity);
            }
            
            String icon = text.equals("Hacking") ? "⚠" : "⚔";
            fontRendererObj.drawStringWithShadow(icon, x + 15, y + OPTION_HEIGHT/2 - 4, 0xFFFFFF);
            fontRendererObj.drawStringWithShadow(text, x + 40, y + OPTION_HEIGHT/2 - 4, 
                hovered ? UIHelper.ACCENT.getRGB() : 0xFFFFFF);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            int startX = (width - windowWidth) / 2;
            int startY = (height - windowHeight) / 2;
            int optionsStartY = startY + 100;

            for (int i = 0; i < options.length; i++) {
                if (mouseX >= startX + 10 && mouseX <= startX + windowWidth - 10 &&
                    mouseY >= optionsStartY + (i * (OPTION_HEIGHT + 8)) && 
                    mouseY < optionsStartY + (i * (OPTION_HEIGHT + 8)) + OPTION_HEIGHT) {
                    
                    UIHelper.playClickSound();

                    if (i == 0) {
                        mc.displayGuiScreen(new HackTypeGui(targetPlayer));
                    } else {
                        mc.displayGuiScreen(new TeamingTypeGui(targetPlayer));
                    }
                    break;
                }
            }
        }
    }

    
    public static class TeamingTypeGui extends BasebannedGui {
        private final EntityPlayer targetPlayer;
        private final String[] gameTypes = {
            "BedWars", "EggWars", "SurvivalGames", "SkyWars", 
            "LuckyBlockWars", "UHC", "GNT", "HackTeaming"
        };
        private static final int GAME_CARD_HEIGHT = 50;
        private static final int VISIBLE_GAMES = 5;
        private int hoveredGame = -1;

        public TeamingTypeGui(EntityPlayer player) {
            this.targetPlayer = player;
            this.windowWidth = 300;
            this.windowHeight = 350;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xCC000000);
            
            int startX = (width - windowWidth) / 2;
            int startY = (height - windowHeight) / 2;
            
            // Main window
            drawRect(startX - 1, startY - 1, startX + windowWidth + 1, startY + windowHeight + 1, 0x50000000);
            drawRect(startX, startY, startX + windowWidth, startY + windowHeight, UIHelper.DARK_BG.getRGB());
            
            // Title area
            drawRect(startX, startY, startX + windowWidth, startY + 50, UIHelper.DARK_CARD.getRGB());
            drawCenteredString(fontRendererObj, "Select Game Type", startX + windowWidth/2, startY + 20, UIHelper.ACCENT.getRGB());
            
            // Player info
            int playerCardY = startY + 60;
            UIHelper.drawRoundedRect(startX + 10, playerCardY, windowWidth - 20, 50, 6, UIHelper.DARK_CARD.getRGB());
            UIHelper.drawPlayerHead(targetPlayer, startX + 20, playerCardY + 9);
            fontRendererObj.drawStringWithShadow(targetPlayer.getName(), startX + 60, playerCardY + 20, 0xFFFFFF);
            
            // Game types list with scissor test
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            double scaleFactor = mc.displayHeight / (double)height;
            GL11.glScissor(
                (int)(startX * scaleFactor),
                (int)(mc.displayHeight - (startY + windowHeight) * scaleFactor),
                (int)(windowWidth * scaleFactor),
                (int)((windowHeight - 130) * scaleFactor)
            );

            int gamesStartY = playerCardY + 60;
            int maxScroll = Math.max(0, gameTypes.length * (GAME_CARD_HEIGHT + 10) - (windowHeight - 180));
            scrollProgress = MathHelper.clamp_float(scrollProgress, 0, maxScroll);

            for (int i = 0; i < gameTypes.length; i++) {
                int cardY = gamesStartY + (i * (GAME_CARD_HEIGHT + 10)) - (int)scrollProgress;
                
                if (cardY + GAME_CARD_HEIGHT >= gamesStartY && cardY <= startY + windowHeight - 20) {
                    boolean isHovered = mouseX >= startX + 10 && mouseX <= startX + windowWidth - 10 &&
                                      mouseY >= cardY && mouseY < cardY + GAME_CARD_HEIGHT;
                    
                    if (isHovered && hoveredGame != i) {
                        hoveredGame = i;
                        hoverStartTime = System.currentTimeMillis();
                    }
                    
                    drawGameTypeCard(startX + 10, cardY, windowWidth - 20, gameTypes[i], isHovered);
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            // Scrollbar
            if (gameTypes.length > VISIBLE_GAMES) {
                float viewableRatio = (windowHeight - 180) / (float)(gameTypes.length * (GAME_CARD_HEIGHT + 10));
                UIHelper.drawModernScrollbar(
                    startX + windowWidth - 4, gamesStartY,
                    4, windowHeight - 180,
                    scrollProgress / maxScroll,
                    viewableRatio
                );
            }
        }

        private void drawGameTypeCard(int x, int y, int width, String gameType, boolean hovered) {
            int bgColor = hovered ? UIHelper.HOVER.getRGB() : UIHelper.DARK_CARD.getRGB();
            UIHelper.drawRoundedRect(x, y, width, GAME_CARD_HEIGHT, 6, bgColor);
            
            if (hovered) {
                float glowIntensity = Math.min(1.0f, (System.currentTimeMillis() - hoverStartTime) / 200.0f);
                UIHelper.drawGlowingBorder(x, y, width, GAME_CARD_HEIGHT, UIHelper.ACCENT.getRGB(), glowIntensity);
            }
            
            // Draw game icon with scaling
            String icon = getGameIcon(gameType);
            GlStateManager.pushMatrix();
            float scale = 2.0F;
            GlStateManager.scale(scale, scale, 1.0F);
            fontRendererObj.drawStringWithShadow(icon, 
                (x + 20) / scale, 
                (y + GAME_CARD_HEIGHT/2 - 8) / scale,
                0xFFFFFF);
            GlStateManager.popMatrix();
            
            fontRendererObj.drawStringWithShadow(gameType, x + 60, y + GAME_CARD_HEIGHT/2 - 4,
                hovered ? UIHelper.ACCENT.getRGB() : 0xFFFFFF);
            
            fontRendererObj.drawStringWithShadow(getGameDescription(gameType), 
                x + 60, y + GAME_CARD_HEIGHT/2 + 8, 0x888888);
        }

        private String getGameIcon(String gameType) {
            switch(gameType.toLowerCase()) {
                case "bedwars": return "⌂";
                case "eggwars": return "○";
                case "survivalgames": return "⚔";
                case "skywars": return "☁";
                case "luckyblockwars": return "?";
                case "uhc": return "❤";
                case "gnt": return "⭐";
                case "hackteaming": return "⚠";
                default: return "✦";
            }
        }

        private String getGameCommand(String gameType) {
            switch(gameType.toLowerCase()) {
                case "bedwars": return "/hteamingbw";
                case "eggwars": return "/hteamingew";
                case "survivalgames": return "/hteamingsg";
                case "skywars": return "/hteamingsw";
                case "luckyblockwars": return "/hteaminglbw";
                case "uhc": return "/hteaminguhc";
                case "gnt": return "/hteaminggnt";
                case "hackteaming": return "/hhackteaming";
                default: return "/hteaming";
            }
        }

        private String getGameDescription(String gameType) {
            switch(gameType.toLowerCase()) {
                case "bedwars": return "Teaming in BedWars";
                case "eggwars": return "Teaming in EggWars";
                case "survivalgames": return "Teaming in Survival Games";
                case "skywars": return "Teaming in SkyWars";
                case "luckyblockwars": return "Teaming in Lucky Block Wars";
                case "uhc": return "Teaming in UHC";
                case "gnt": return "Teaming in GNT";
                case "hackteaming": return "Teaming with hackers";
                default: return "";
            }
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
            handleScroll(Math.max(0, gameTypes.length * (GAME_CARD_HEIGHT + 10) - (windowHeight - 180)));
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            int startX = (width - windowWidth) / 2;
            int startY = (height - windowHeight) / 2;
            int gamesStartY = startY + 120;
            
            for (int i = 0; i < gameTypes.length; i++) {
                int cardY = gamesStartY + (i * (GAME_CARD_HEIGHT + 10)) - (int)scrollProgress;
                
                if (mouseX >= startX + 10 && mouseX <= startX + windowWidth - 10 &&
                    mouseY >= cardY && mouseY < cardY + GAME_CARD_HEIGHT) {
                    
                    UIHelper.playClickSound();
                    String command = getGameCommand(gameTypes[i]);
                    mc.thePlayer.sendChatMessage(command + " " + targetPlayer.getName());
                    mc.displayGuiScreen(null);
                    return;
                }
            }
            
            if (mouseX >= width/2 + windowWidth/2 - 4 && 
                mouseX <= width/2 + windowWidth/2 &&
                mouseY >= gamesStartY && 
                mouseY <= startY + windowHeight - 20) {
                isDragging = true;
                lastMouseY = mouseY;
            }
        }

        @Override
        protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
            if (isDragging) {
                int diff = mouseY - lastMouseY;
                scrollProgress += diff * 2.5f;
                int maxScroll = Math.max(0, gameTypes.length * (GAME_CARD_HEIGHT + 10) - (windowHeight - 180));
                scrollProgress = MathHelper.clamp_float(scrollProgress, 0, maxScroll);
                lastMouseY = mouseY;
            }
        }
    }

    
    public static class HackTypeGui extends BasebannedGui {
        private final EntityPlayer targetPlayer;
        private final String[] hackTypes = {
            "Fly", "KillAura", "Aimbot", "AntiKnockback",
            "AutoArmor", "NoFall"
        };
        private static final int HACK_CARD_HEIGHT = 60;
        private static final int VISIBLE_HACKS = 4;
        private int hoveredHack = -1;

        public HackTypeGui(EntityPlayer player) {
            this.targetPlayer = player;
            this.windowWidth = 300;
            this.windowHeight = 350;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0xCC000000);
            
            int startX = (width - windowWidth) / 2;
            int startY = (height - windowHeight) / 2;
            
            // Main window with shadow
            drawRect(startX - 1, startY - 1, startX + windowWidth + 1, startY + windowHeight + 1, 0x50000000);
            drawRect(startX, startY, startX + windowWidth, startY + windowHeight, UIHelper.DARK_BG.getRGB());
            
            // Title area
            drawRect(startX, startY, startX + windowWidth, startY + 50, UIHelper.DARK_CARD.getRGB());
            drawCenteredString(fontRendererObj, "Select Hack Type", startX + windowWidth/2, startY + 20, UIHelper.ACCENT.getRGB());
            
            // Player info card
            int playerCardY = startY + 60;
            UIHelper.drawRoundedRect(startX + 10, playerCardY, windowWidth - 20, 50, 6, UIHelper.DARK_CARD.getRGB());
            UIHelper.drawPlayerHead(targetPlayer, startX + 20, playerCardY + 9);
            fontRendererObj.drawStringWithShadow(targetPlayer.getName(), startX + 60, playerCardY + 20, 0xFFFFFF);
            String distanceText = String.format("%.0f blocks away", targetPlayer.getDistanceToEntity(mc.thePlayer));
            fontRendererObj.drawStringWithShadow(distanceText, startX + 60, playerCardY + 32, 0x888888);
            
            // Hack types list with scissor test
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            double scaleFactor = mc.displayHeight / (double)height;
            GL11.glScissor(
                (int)(startX * scaleFactor),
                (int)(mc.displayHeight - (startY + windowHeight) * scaleFactor),
                (int)(windowWidth * scaleFactor),
                (int)((windowHeight - 130) * scaleFactor)
            );

            int hacksStartY = playerCardY + 60;
            int maxScroll = Math.max(0, hackTypes.length * (HACK_CARD_HEIGHT + 10) - (windowHeight - 180));
            scrollProgress = MathHelper.clamp_float(scrollProgress, 0, maxScroll);

            for (int i = 0; i < hackTypes.length; i++) {
                int cardY = hacksStartY + (i * (HACK_CARD_HEIGHT + 10)) - (int)scrollProgress;
                
                if (cardY + HACK_CARD_HEIGHT >= hacksStartY && cardY <= startY + windowHeight - 20) {
                    boolean isHovered = mouseX >= startX + 10 && mouseX <= startX + windowWidth - 10 &&
                                      mouseY >= cardY && mouseY < cardY + HACK_CARD_HEIGHT;
                    
                    if (isHovered && hoveredHack != i) {
                        hoveredHack = i;
                        hoverStartTime = System.currentTimeMillis();
                    }
                    
                    drawHackTypeCard(startX + 10, cardY, windowWidth - 20, hackTypes[i], isHovered);
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            // Scrollbar
            if (hackTypes.length > VISIBLE_HACKS) {
                float viewableRatio = (windowHeight - 180) / (float)(hackTypes.length * (HACK_CARD_HEIGHT + 10));
                UIHelper.drawModernScrollbar(
                    startX + windowWidth - 4, hacksStartY,
                    4, windowHeight - 180,
                    scrollProgress / maxScroll,
                    viewableRatio
                );
            }
        }

        private void drawHackTypeCard(int x, int y, int width, String hackType, boolean hovered) {
            int bgColor = hovered ? UIHelper.HOVER.getRGB() : UIHelper.DARK_CARD.getRGB();
            UIHelper.drawRoundedRect(x, y, width, HACK_CARD_HEIGHT, 6, bgColor);
            
            if (hovered) {
                float glowIntensity = Math.min(1.0f, (System.currentTimeMillis() - hoverStartTime) / 200.0f);
                UIHelper.drawGlowingBorder(x, y, width, HACK_CARD_HEIGHT, UIHelper.ACCENT.getRGB(), glowIntensity);
            }
            
            // Increased scale for larger icons
            String icon = getHackTypeIcon(hackType);
            GlStateManager.pushMatrix();
            float scale = 3.0F;  // Increased from 2.0F to 3.0F for larger icons
            GlStateManager.scale(scale, scale, 1.0F);
            fontRendererObj.drawStringWithShadow(icon, 
                (x + 20) / scale, 
                (y + HACK_CARD_HEIGHT/2 - 12) / scale,  // Adjusted y position to center the larger icon
                0xFFFFFF);
            GlStateManager.popMatrix();
            
            // Adjusted text position to accommodate larger icon
            fontRendererObj.drawStringWithShadow(hackType, x + 80, y + HACK_CARD_HEIGHT/2 - 4,  // Increased x offset from 60 to 80
                hovered ? UIHelper.ACCENT.getRGB() : 0xFFFFFF);
            
            fontRendererObj.drawStringWithShadow(getHackDescription(hackType), 
                x + 80, y + HACK_CARD_HEIGHT/2 + 8,  // Increased x offset from 60 to 80
                0x888888);
        }

        private String getHackTypeIcon(String hackType) {
            switch(hackType.toLowerCase()) {
                case "fly": return "↑";       // Upward arrow
                case "killaura": return "†";   // Cross symbol
                case "aimbot": return "⊕";     // Circled plus
                case "antiknockback": return "■"; // Square block
                case "autoarmor": return "▣";  // Square with inner fill
                case "nofall": return "△";     // Triangle
                case "automine": return "⚒";   // Hammer and pick
                default: return "?";
            }
        }

        private String getHackDescription(String hackType) {
            switch(hackType.toLowerCase()) {
                case "fly": return "ANY MOVEMENT HACKS";
                case "killaura": return "ANY HITTING HACKS";
                case "aimbot": return "AIMBOT";
                case "antiknockback": return "KNOCKBACK";
                case "autoarmor": return "REPLACING ARMOR";
                case "nofall": return "NO FALL DAMAGE";
                default: return "";
            }
        }

        private String getHackCommand(String hackType) {
            switch(hackType.toLowerCase()) {
                case "fly": return "/hfly";
                case "killaura": return "/hkillaura";
                case "aimbot": return "/haimbot";
                case "antiknockback": return "/hnoknockback";
                case "autoarmor": return "/hautoarmor";
                case "nofall": return "/hnofall";
                case "automine": return "/hautomine";
                default: return "/hban";
            }
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
            handleScroll(Math.max(0, hackTypes.length * (HACK_CARD_HEIGHT + 10) - (windowHeight - 180)));
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            int startX = (width - windowWidth) / 2;
            int startY = (height - windowHeight) / 2;
            int hacksStartY = startY + 120;
            
            for (int i = 0; i < hackTypes.length; i++) {
                int cardY = hacksStartY + (i * (HACK_CARD_HEIGHT + 10)) - (int)scrollProgress;
                
                if (mouseX >= startX + 10 && mouseX <= startX + windowWidth - 10 &&
                    mouseY >= cardY && mouseY < cardY + HACK_CARD_HEIGHT) {
                    
                    UIHelper.playClickSound();
                    String command = getHackCommand(hackTypes[i]);
                    mc.thePlayer.sendChatMessage(command + " " + targetPlayer.getName());
                    mc.displayGuiScreen(null);
                    return;
                }
            }
            
            // Handle scrollbar dragging
            if (mouseX >= width/2 + windowWidth/2 - 4 && 
                mouseX <= width/2 + windowWidth/2 &&
                mouseY >= hacksStartY && 
                mouseY <= startY + windowHeight - 20) {
                isDragging = true;
                lastMouseY = mouseY;
            }
        }

        @Override
        protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
            if (isDragging) {
                int diff = mouseY - lastMouseY;
                scrollProgress += diff * 2.5f;
                int maxScroll = Math.max(0, hackTypes.length * (HACK_CARD_HEIGHT + 10) - (windowHeight - 180));
                scrollProgress = MathHelper.clamp_float(scrollProgress, 0, maxScroll);
                lastMouseY = mouseY;
            }
        }
    }
}