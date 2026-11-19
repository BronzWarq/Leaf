package org.dreeam.leaf.world.block;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import static net.minecraft.world.level.block.Block.*;
import static net.minecraft.world.level.block.PoweredRailBlock.POWERED;
import static net.minecraft.world.level.block.PoweredRailBlock.SHAPE;

public class OptimizedPoweredRails {

    private static final Direction[] EAST_WEST_DIR = new Direction[]{Direction.WEST, Direction.EAST};
    private static final Direction[] NORTH_SOUTH_DIR = new Direction[]{Direction.SOUTH, Direction.NORTH};

    private static final int UPDATE_FORCE_PLACE = UPDATE_MOVE_BY_PISTON | UPDATE_KNOWN_SHAPE | UPDATE_CLIENTS;

    private static void giveShapeUpdate(Level level, BlockState state, BlockPos pos, BlockPos fromPos, Direction direction) {
        BlockState oldState = level.getBlockState(pos);
        Block.updateOrDestroy(
            oldState,
            oldState.updateShape(level, level, pos, direction.getOpposite(), fromPos, state, level.getRandom()),
            level,
            pos,
            UPDATE_CLIENTS & -34,
            0
        );
    }

    public static void updateState(PoweredRailBlock self, BlockState state, Level level, BlockPos pos) {
        boolean shouldBePowered = level.hasNeighborSignal(pos) ||
            self.findPoweredRailSignal(level, pos, state, true, 0) ||
            self.findPoweredRailSignal(level, pos, state, false, 0);
        if (shouldBePowered != state.getValue(POWERED)) {
            RailShape railShape = state.getValue(SHAPE);
            if (railShape.isSlope()) {
                level.setBlock(pos, state.setValue(POWERED, shouldBePowered), 3);
                level.updateNeighborsAtExceptFromFacing(pos.below(), self, Direction.UP, null);
                level.updateNeighborsAtExceptFromFacing(pos.above(), self, Direction.DOWN, null); // isSlope
            } else if (shouldBePowered) {
                powerLane(self, level, pos, state, railShape);
            } else {
                dePowerLane(self, level, pos, state, railShape);
            }
        }
    }

    private static boolean findPoweredRailSignalFaster(PoweredRailBlock self, Level level, BlockPos pos,
                                                       boolean searchForward, int distance, RailShape shape,
                                                       Long2BooleanOpenHashMap checkedPos) {
        BlockState blockState = level.getBlockState(pos);
        final long posKey = pos.asLong();
        boolean speedCheck = checkedPos.containsKey(posKey) && checkedPos.get(posKey);
        if (speedCheck) {
            return level.hasNeighborSignal(pos) ||
                findPoweredRailSignalFaster(self, level, pos, blockState, searchForward, distance + 1, checkedPos);
        } else {
            if (blockState.is(self)) {
                RailShape railShape = blockState.getValue(SHAPE);
                if (shape == RailShape.EAST_WEST && (
                    railShape == RailShape.NORTH_SOUTH ||
                        railShape == RailShape.ASCENDING_NORTH ||
                        railShape == RailShape.ASCENDING_SOUTH
                ) || shape == RailShape.NORTH_SOUTH && (
                    railShape == RailShape.EAST_WEST ||
                        railShape == RailShape.ASCENDING_EAST ||
                        railShape == RailShape.ASCENDING_WEST
                )) {
                    return false;
                } else if (blockState.getValue(POWERED)) {
                    return level.hasNeighborSignal(pos) ||
                        findPoweredRailSignalFaster(self, level, pos, blockState, searchForward, distance + 1, checkedPos);
                } else {
                    return false;
                }
            }
            return false;
        }
    }

    private static boolean findPoweredRailSignalFaster(PoweredRailBlock self, Level level,
                                                       BlockPos pos, BlockState state, boolean searchForward, int distance,
                                                       Long2BooleanOpenHashMap checkedPos) {
        if (distance >= level.purpurConfig.railActivationRange)
            return false; // Purpur - Config for powered rail activation distance
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean flag = true;
        RailShape railShape = state.getValue(SHAPE);
        switch (railShape.ordinal()) {
            case 0 -> {
                if (searchForward) z++;
                else z--;
            }
            case 1 -> {
                if (searchForward) x--;
                else x++;
            }
            case 2 -> {
                if (searchForward) {
                    x--;
                } else {
                    x++;
                    y++;
                    flag = false;
                }
                railShape = RailShape.EAST_WEST;
            }
            case 3 -> {
                if (searchForward) {
                    x--;
                    y++;
                    flag = false;
                } else {
                    x++;
                }
                railShape = RailShape.EAST_WEST;
            }
            case 4 -> {
                if (searchForward) {
                    z++;
                } else {
                    z--;
                    y++;
                    flag = false;
                }
                railShape = RailShape.NORTH_SOUTH;
            }
            case 5 -> {
                if (searchForward) {
                    z++;
                    y++;
                    flag = false;
                } else {
                    z--;
                }
                railShape = RailShape.NORTH_SOUTH;
            }
        }
        return findPoweredRailSignalFaster(
            self, level, new BlockPos(x, y, z),
            searchForward, distance, railShape, checkedPos
        ) ||
            (flag && findPoweredRailSignalFaster(
                self, level, new BlockPos(x, y - 1, z),
                searchForward, distance, railShape, checkedPos
            ));
    }

    private static void powerLane(PoweredRailBlock self, Level level, BlockPos pos,
                                  BlockState mainState, RailShape railShape) {
        level.setBlock(pos, mainState.setValue(POWERED, true), UPDATE_FORCE_PLACE);
        Long2BooleanOpenHashMap checkedPos = new Long2BooleanOpenHashMap();
        checkedPos.put(pos.asLong(), true);
        if (railShape == RailShape.NORTH_SOUTH) { // Order: +z, -z
            final int count0 = setRailPositionsPower(self, level, pos, checkedPos, NORTH_SOUTH_DIR[0]);
            final int count1 = setRailPositionsPower(self, level, pos, checkedPos, NORTH_SOUTH_DIR[1]);
            updateRails(self, false, level, pos, mainState, count0, count1);
        } else if (railShape == RailShape.EAST_WEST) { // Order: -x, +x
            final int count0 = setRailPositionsPower(self, level, pos, checkedPos, EAST_WEST_DIR[0]);
            final int count1 = setRailPositionsPower(self, level, pos, checkedPos, EAST_WEST_DIR[1]);
            updateRails(self, true, level, pos, mainState, count0, count1);
        }
    }

    private static void dePowerLane(PoweredRailBlock self, Level level, BlockPos pos,
                                    BlockState mainState, RailShape railShape) {
        level.setBlock(pos, mainState.setValue(POWERED, false), UPDATE_FORCE_PLACE);
        if (railShape == RailShape.NORTH_SOUTH) { // Order: +z, -z
            final int count0 = setRailPositionsDePower(self, level, pos, NORTH_SOUTH_DIR[0]);
            final int count1 = setRailPositionsDePower(self, level, pos, NORTH_SOUTH_DIR[1]);
            updateRails(self, false, level, pos, mainState, count0, count1);
        } else if (railShape == RailShape.EAST_WEST) { // Order: -x, +x
            final int count0 = setRailPositionsDePower(self, level, pos, EAST_WEST_DIR[0]);
            final int count1 = setRailPositionsDePower(self, level, pos, EAST_WEST_DIR[1]);
            updateRails(self, true, level, pos, mainState, count0, count1);
        }
    }

    private static int setRailPositionsPower(PoweredRailBlock self, Level level, BlockPos pos,
                                             Long2BooleanOpenHashMap checkedPos, Direction dir) {
        final int railPowerLimit = level.purpurConfig.railActivationRange;
        int count = 0;
        for (int z = 1; z < railPowerLimit; z++) {
            BlockPos newPos = pos.relative(dir, z);
            BlockState state = level.getBlockState(newPos);
            final long newPosKey = newPos.asLong();
            if (checkedPos.containsKey(newPosKey)) {
                if (!checkedPos.get(newPosKey))
                    break;
                count++;
            } else if (!state.is(self) || state.getValue(POWERED) || !(level.hasNeighborSignal(newPos) ||
                findPoweredRailSignalFaster(self, level, newPos, state, true, 0, checkedPos) ||
                findPoweredRailSignalFaster(self, level, newPos, state, false, 0, checkedPos))) {
                checkedPos.put(newPosKey, false);
                break;
            } else {
                checkedPos.put(newPosKey, true);
                if (!state.getValue(POWERED)) {
                    level.setBlock(newPos, state.setValue(POWERED, true), UPDATE_FORCE_PLACE);
                }
                count++;
            }
        }
        return count;
    }

    private static int setRailPositionsDePower(PoweredRailBlock self, Level level, BlockPos pos, Direction dir) {
        final int railPowerLimit = level.purpurConfig.railActivationRange;
        int count = 0;
        for (int z = 1; z < railPowerLimit; z++) {
            BlockPos newPos = pos.relative(dir, z);
            BlockState state = level.getBlockState(newPos);
            if (!state.is(self) || !state.getValue(POWERED) || level.hasNeighborSignal(newPos) ||
                self.findPoweredRailSignal(level, newPos, state, true, 0) ||
                self.findPoweredRailSignal(level, newPos, state, false, 0))
                break;
            if (state.getValue(POWERED)) {
                level.setBlock(newPos, state.setValue(POWERED, false), UPDATE_FORCE_PLACE);
            }
            count++;
        }
        return count;
    }

    private static void shapeUpdateEnd(PoweredRailBlock self, Level level, BlockPos pos, BlockState mainState,
                                       int endPos, Direction direction, int currentPos, BlockPos blockPos) {
        if (currentPos == endPos) {
            BlockPos newPos = pos.relative(direction, currentPos + 1);
            giveShapeUpdate(level, mainState, newPos, pos, direction);
            BlockState state = level.getBlockState(blockPos);
            if (state.is(self) && state.getValue(SHAPE).isSlope()) giveShapeUpdate(level, mainState, newPos.above(), pos, direction);
        }
    }

    private static void neighborUpdateEnd(PoweredRailBlock self, Level level, BlockPos pos, int endPos,
                                          Direction direction, Block block, int currentPos, BlockPos blockPos) {
        if (currentPos == endPos) {
            BlockPos newPos = pos.relative(direction, currentPos + 1);
            level.neighborChanged(newPos, block, null);
            BlockState state = level.getBlockState(blockPos);
            if (state.is(self) && state.getValue(SHAPE).isSlope()) level.neighborChanged(newPos.above(), block, null);
        }
    }

    private static void updateRailsSectionEastWestShape(PoweredRailBlock self, Level level, BlockPos pos,
                                                        int c, BlockState mainState, Direction dir,
                                                        int count1, int countAmt) {
        BlockPos pos1 = pos.relative(dir, c);
        if (c == 0 && count1 == 0) giveShapeUpdate(level, mainState, pos1.relative(dir.getOpposite()), pos, dir.getOpposite());
        shapeUpdateEnd(self, level, pos, mainState, countAmt, dir, c, pos1);
        giveShapeUpdate(level, mainState, pos1.below(), pos, Direction.DOWN);
        giveShapeUpdate(level, mainState, pos1.above(), pos, Direction.UP);
        giveShapeUpdate(level, mainState, pos1.north(), pos, Direction.NORTH);
        giveShapeUpdate(level, mainState, pos1.south(), pos, Direction.SOUTH);
    }

    private static void updateRailsSectionNorthSouthShape(PoweredRailBlock self, Level level, BlockPos pos,
                                                          int c, BlockState mainState, Direction dir,
                                                          int count1, int countAmt) {
        BlockPos pos1 = pos.relative(dir, c);
        giveShapeUpdate(level, mainState, pos1.west(), pos, Direction.WEST);
        giveShapeUpdate(level, mainState, pos1.east(), pos, Direction.EAST);
        giveShapeUpdate(level, mainState, pos1.below(), pos, Direction.DOWN);
        giveShapeUpdate(level, mainState, pos1.above(), pos, Direction.UP);
        shapeUpdateEnd(self, level, pos, mainState, countAmt, dir, c, pos1);
        if (c == 0 && count1 == 0) giveShapeUpdate(level, mainState, pos1.relative(dir.getOpposite()), pos, dir.getOpposite());
    }

    private static void updateRails(PoweredRailBlock self, boolean eastWest, Level level,
                                    BlockPos pos, BlockState mainState, int count0, int count1) {
        if (eastWest) {
            for (int i = 0; i < EAST_WEST_DIR.length; ++i) {
                int countAmt = i == 0 ? count0 : count1;
                if (i == 1 && countAmt == 0) continue;
                Direction dir = EAST_WEST_DIR[i];
                Block block = mainState.getBlock();
                for (int c = countAmt; c >= i; c--) {
                    BlockPos p = pos.relative(dir, c);
                    if (c == 0 && count1 == 0) level.neighborChanged(p.relative(dir.getOpposite()), block, null);
                    neighborUpdateEnd(self, level, pos, countAmt, dir, block, c, p);
                    level.neighborChanged(p.below(), block, null);
                    level.neighborChanged(p.above(), block, null);
                    level.neighborChanged(p.north(), block, null);
                    level.neighborChanged(p.south(), block, null);
                    BlockPos pos2 = pos.relative(dir, c).below();
                    level.neighborChanged(pos2.below(), block, null);
                    level.neighborChanged(pos2.north(), block, null);
                    level.neighborChanged(pos2.south(), block, null);
                    if (c == countAmt) level.neighborChanged(pos.relative(dir, c + 1).below(), block, null);
                    if (c == 0 && count1 == 0) level.neighborChanged(p.relative(dir.getOpposite()).below(), block, null);
                }
                for (int c = countAmt; c >= i; c--)
                    updateRailsSectionEastWestShape(self, level, pos, c, mainState, dir, count1, countAmt);
            }
        } else {
            for (int i = 0; i < NORTH_SOUTH_DIR.length; ++i) {
                int countAmt = i == 0 ? count0 : count1;
                if (i == 1 && countAmt == 0) continue;
                Direction dir = NORTH_SOUTH_DIR[i];
                Block block = mainState.getBlock();
                for (int c = countAmt; c >= i; c--) {
                    BlockPos p = pos.relative(dir, c);
                    level.neighborChanged(p.west(), block, null);
                    level.neighborChanged(p.east(), block, null);
                    level.neighborChanged(p.below(), block, null);
                    level.neighborChanged(p.above(), block, null);
                    neighborUpdateEnd(self, level, pos, countAmt, dir, block, c, p);
                    if (c == 0 && count1 == 0) level.neighborChanged(p.relative(dir.getOpposite()), block, null);
                    BlockPos pos2 = pos.relative(dir, c).below();
                    level.neighborChanged(pos2.west(), block, null);
                    level.neighborChanged(pos2.east(), block, null);
                    level.neighborChanged(pos2.below(), block, null);
                    if (c == countAmt) level.neighborChanged(pos.relative(dir, c + 1).below(), block, null);
                    if (c == 0 && count1 == 0) level.neighborChanged(p.relative(dir.getOpposite()).below(), block, null);
                }
                for (int c = countAmt; c >= i; c--)
                    updateRailsSectionNorthSouthShape(self, level, pos, c, mainState, dir, count1, countAmt);
            }
        }
    }
}
