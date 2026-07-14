package com.example.wrestlingmania.block;

import com.example.wrestlingmania.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/** Stores the table's remaining hit points (3 by default). */
public class BreakableTableBlockEntity extends BlockEntity {
	public static final int MAX_HITS = 3;
	private int hitsRemaining = MAX_HITS;

	public BreakableTableBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BREAKABLE_TABLE, pos, state);
	}

	/** @return true when the table should shatter. */
	public boolean takeHit() {
		this.hitsRemaining--;
		markDirty();
		return this.hitsRemaining <= 0;
	}

	public int getHitsRemaining() {
		return this.hitsRemaining;
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.putInt("HitsRemaining", this.hitsRemaining);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		if (nbt.contains("HitsRemaining")) {
			this.hitsRemaining = nbt.getInt("HitsRemaining");
		}
	}
}
