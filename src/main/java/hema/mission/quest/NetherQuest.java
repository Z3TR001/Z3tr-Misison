package hema.mission.quest;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.ArrayList;

public class NetherQuest implements Quest{

    @Override
    public String title() {
        return "Obtain a Lava Bucket to go to The Nether ";
    }

    @Override
    public String note() {
        return "Note: You will be teleported once you get the lava bucket";
    }

    @Override
    public int goal() {
        return 1;
    }

    @Override
    public String rewardText() {
        return "Welcome to the Nether";
    }

    @Override
    public void registerEvents() {

        ServerTickEvents.END_SERVER_TICK.register(server ->{
            if (server.getTicks() % 10 != 0)

                return;

            for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
                if (QuestManager.isCurrent(player, this) && holdingLava(player)) {
                    QuestManager.add(player,this);
                }
            }
        });
    }
    private static boolean holdingLava(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.LAVA_BUCKET)
                || player.getOffHandStack().isOf(Items.LAVA_BUCKET);
    }
    @Override
    public void reward(ServerPlayerEntity player) {

        player.getInventory().offerOrDrop(new ItemStack(Items.FLINT_AND_STEEL));
        player.getInventory().offerOrDrop(new ItemStack(Items.OBSIDIAN, 10));

        if (player.getWorld().getRegistryKey() == World.NETHER)
            return;

        ServerWorld nether = player.server.getWorld(World.NETHER);
        if (nether == null )
            return;

        BlockPos point = SafePoint(nether, player.getBlockX() / 8, player.getBlockZ() / 8);

        player.teleportTo(new TeleportTarget(nether, Vec3d.ofBottomCenter(point), Vec3d.ZERO, player.getYaw(), player.getPitch(), TeleportTarget.NO_OP
        ));
    }

    private static BlockPos SafePoint(ServerWorld world, int centerX, int centerZ) {
        int [] offsets = {0 , 4 , -4 , 8 , -8};

        for (int dx : offsets) {
            for (int dz : offsets) {
                int x = centerX + dx;
                int z = centerZ + dz;
                world.getChunk(x >> 4 , z >> 4);

                 for (int y = 120; y > 32; y--) {
                     BlockPos feet = new BlockPos(x,y,z);
                     BlockPos floorpos = feet.down();
                     BlockState floor = world.getBlockState(floorpos);


                     if (world.getBlockState(feet).isAir() &&
                     world.getBlockState(feet.up()).isAir()
                     && floor.isSideSolidFullSquare(world, floorpos, Direction.UP)
                             && !floor.isOf(Blocks.MAGMA_BLOCK)) {
                         return feet;
                     }
                 }
            }
        }
        BlockPos base = new BlockPos(centerX, 70, centerZ);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(base.add(dx, -1 , dz), Blocks.NETHERRACK.getDefaultState());

                for (int dy = 0; dy <= 2; dy++) {
                    world.setBlockState(base.add(dx, dy, dz), Blocks.AIR.getDefaultState());
                }
            }
        }
        return base;
    }
}
