package com.pedrorok.hypertube.managers.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record SimpleConnection(BlockPos pos, Direction direction) {
    public static final Codec<SimpleConnection> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(SimpleConnection::pos),
            Direction.CODEC.fieldOf("direction").forGetter(SimpleConnection::direction)
    ).apply(i, SimpleConnection::new));
}
