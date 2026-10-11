package hema.mission.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import hema.mission.Mission;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public final class QuestManager {

    public static final List<Quest> QUESTS = List.of(
            new WoodQuest(),
            new KillQuest(),
            new FlowerQuest(),
            new NetherQuest(),
            new EndQuest()




    );

    public record Progress(int quest, int progress) {
        public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("quest").forGetter(Progress::quest),
                Codec.INT.fieldOf("progress").forGetter(Progress::progress)
        ).apply(i, Progress::new));
    }

    public static final AttachmentType<Progress> DATA = AttachmentRegistry.<Progress>builder()
            .persistent(Progress.CODEC)
            .copyOnDeath()
            .initializer(() -> new Progress(0, 0))
            .buildAndRegister(Identifier.of(Mission.MOD_ID, "progress"));

    public record SyncPayload(int quest, int progress, boolean completed) implements CustomPayload {
        public static final CustomPayload.Id<SyncPayload> ID =
                new CustomPayload.Id<>(Identifier.of(Mission.MOD_ID, "sync"));

        public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, SyncPayload::quest,
                PacketCodecs.VAR_INT, SyncPayload::progress,
                PacketCodecs.BOOL, SyncPayload::completed,
                SyncPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player, false));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> send(newPlayer, false));

        QUESTS.forEach(Quest::registerEvents);
    }

    public static boolean isCurrent(ServerPlayerEntity player, Quest quest) {
        Progress p = player.getAttachedOrCreate(DATA);
        return p.quest() < QUESTS.size() && QUESTS.get(p.quest()) == quest;
    }

    public static void add(ServerPlayerEntity player, Quest quest) {
        if (!isCurrent(player, quest)) return;
        setProgress(player, quest, player.getAttachedOrCreate(DATA).progress() + 1);
    }

    public static void setProgress(ServerPlayerEntity player, Quest quest, int amount) {
        if (!isCurrent(player, quest)) return;

        Progress p = player.getAttachedOrCreate(DATA);
        if (amount <= p.progress()) return;

        if (amount >= quest.goal()) {
            quest.reward(player);
            player.setAttached(DATA, new Progress(p.quest() + 1, 0));
            send(player, true);
        } else {
            player.setAttached(DATA, new Progress(p.quest(), amount));
            send(player, false);
        }
    }

    public static void send(ServerPlayerEntity player, boolean completed) {
        Progress p = player.getAttachedOrCreate(DATA);
        ServerPlayNetworking.send(player, new SyncPayload(p.quest(), p.progress(), completed));
    }

    private QuestManager() {}
}