package hema.mission.quest;

import net.minecraft.server.network.ServerPlayerEntity;

public interface Quest {
    String title();
    String note();
    int goal();
    String rewardText();
    void reward(ServerPlayerEntity player);
    void registerEvents();

}