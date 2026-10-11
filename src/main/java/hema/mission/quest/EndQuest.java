package hema.mission.quest;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.ArrayList;

public class EndQuest implements Quest {

    private static final BlockPos END_SPAWN = new BlockPos(100, 50, 0);

    @Override
    public String title() {
        return "Hold Blaze Rods to be sent to The End";
    }

    @Override
    public String note() {
        return "Note: You will be teleported once you hold the blaze rods";
    }

    @Override
    public int goal() {
        return 1;
    }

    @Override
    public String rewardText() {
        return "Welcome to The End";
    }

    @Override
    public void registerEvents() {

        ServerTickEvents.END_SERVER_TICK.register(server -> {

            if (server.getTicks() % 10 != 0)

                return;

            for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
                if (QuestManager.isCurrent(player, this) && holdingRods(player)) {
                    QuestManager.add(player, this);
                }
            }
        });
    }

    private static boolean holdingRods(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.BLAZE_ROD)
                || player.getOffHandStack().isOf(Items.BLAZE_ROD);
    }

    @Override
    public void reward(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() == World.END)

            return;

        ServerWorld end = player.server.getWorld(World.END);
        if (end == null) return;

        buildPlatform(end);

        player.teleportTo(new TeleportTarget(end, Vec3d.ofBottomCenter(END_SPAWN), Vec3d.ZERO, player.getYaw(), player.getPitch(),
                TeleportTarget.NO_OP
        ));
    }

    private static void buildPlatform(ServerWorld world) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(END_SPAWN.add(dx, -1, dz), Blocks.OBSIDIAN.getDefaultState());

                for (int dy = 0; dy <= 2; dy++) {
                    world.setBlockState(END_SPAWN.add(dx, dy, dz), Blocks.AIR.getDefaultState());
                }
            }
        }
    }
}