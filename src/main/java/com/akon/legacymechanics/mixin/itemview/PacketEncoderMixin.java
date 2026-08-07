package com.akon.legacymechanics.mixin.itemview;

import com.akon.legacymechanics.network.OutgoingItemStacks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Establishes which player a packet is being written for, so items on the wire can be
 * rewritten per-recipient. Changes nothing on its own -- it only supplies the recipient that
 * {@link com.akon.legacymechanics.mixin.itemview.ItemStackOptionalStreamCodecMixin} consumes.
 */
@Mixin(PacketEncoder.class)
public abstract class PacketEncoderMixin<T extends PacketListener> {

    /**
     * {@code @WrapMethod} rather than paired HEAD/RETURN injections: the binding has to be
     * cleared on the exception path too, and {@code @At("RETURN")} does not run when the
     * target throws -- which it does routinely here, via SkipPacketEncoderException and
     * PacketTooLargeException. A leaked binding would misattribute the next packet encoded on
     * this thread to the wrong player.
     */
    @WrapMethod(
        method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V"
    )
    private void bindItemStackRecipient(
        ChannelHandlerContext ctx,
        Packet<T> packet,
        ByteBuf out,
        Operation<Void> original
    ) throws Exception {
        // Connection is itself the pipeline's "packet_handler" entry; getPlayer() is a Paper
        // addition that returns null until the listener becomes a game listener.
        Connection connection = ctx.pipeline().get(Connection.class);
        OutgoingItemStacks.bindRecipient(connection == null ? null : connection.getPlayer());
        try {
            original.call(ctx, packet, out);
        } finally {
            OutgoingItemStacks.clearRecipient();
        }
    }
}
