package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo da posição em grade (linhas/colunas/centralização) dos produtos da
 * loja, respeitando slots absolutos, quebras de linha, páginas e o layout da
 * categoria ativa.
 */
final class ShopSlotGrid {

    static final int ITEMS_START = 9;
    static final int ITEMS_END = 44;
    static final int ITEMS_PER_PAGE = 36;

    private final ShopGui gui;

    ShopSlotGrid(final ShopGui gui) {
        this.gui = gui;
    }

    List<Integer> computeSlots(final List<Object> entries, final int page) {
        final List<Integer> slots = new ArrayList<>();
        final int startIndex = page * ITEMS_PER_PAGE;
        final int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());
        final int count = endIndex - startIndex;

        final boolean vertical = this.gui.currentCategory != null
                && "column".equals(this.gui.currentCategory.getLayoutType());
        final boolean center = this.gui.currentCategory != null && this.gui.currentCategory.isCentered();

        final int[] gridRows = new int[count];
        final int[] gridCols = new int[count];
        final boolean[] placed = new boolean[count];
        final boolean[] absolute = new boolean[count];

        int row = 0;
        int col = 0;

        for (int i = startIndex; i < endIndex; i++) {
            int idx = i - startIndex;
            Object entry = entries.get(i);

            if (entry instanceof ShopItem item) {
                if (item.getAbsolute() != null) {
                    int slot = item.getAbsolute();
                    if (slot >= ITEMS_START && slot <= ITEMS_END) {
                        gridRows[idx] = (slot - ITEMS_START) / 9;
                        gridCols[idx] = (slot - ITEMS_START) % 9;
                        placed[idx] = true;
                        absolute[idx] = true;
                    }
                    continue;
                }
                if (item.getLinebreak() != null) {
                    switch (item.getLinebreak().toLowerCase()) {
                        case "before" -> {
                            row++;
                            col = 0;
                        }
                        case "after" -> {
                            placeItem(gridRows, gridCols, placed, idx, row, col);
                            row++;
                            col = 0;
                            continue;
                        }
                        case "both" -> {
                            row++;
                            col = 0;
                            placeItem(gridRows, gridCols, placed, idx, row, col);
                            row++;
                            col = 0;
                            continue;
                        }
                        default -> { }
                    }
                }
                if (item.getPagebreak() != null) {
                    continue;
                }
                if (!vertical) {
                    if (item.getColumn() != null) {
                        col = item.getColumn();
                    }
                    if (item.getRow() != null) {
                        row = item.getRow() - 1;
                        col = 0;
                    }
                }
                if (item.getSkip() > 0) {
                    if (vertical) {
                        row += item.getSkip();
                    } else {
                        col += item.getSkip();
                    }
                }
            }

            if (!vertical) {
                if (col > 8) {
                    row++;
                    col = 0;
                }
                if (row <= 3) {
                    placeItem(gridRows, gridCols, placed, idx, row, col);
                    col++;
                    if (col >= 9) {
                        row++;
                        col = 0;
                    }
                }
            } else {
                if (row > 3) {
                    row = 0;
                    col++;
                }
                if (col <= 8) {
                    placeItem(gridRows, gridCols, placed, idx, row, col);
                    row++;
                    if (row > 3) {
                        row = 0;
                        col++;
                    }
                }
            }
        }

        if (center) {
            centerGrid(gridRows, gridCols, placed, absolute, vertical);
        }

        for (int i = 0; i < count; i++) {
            if (placed[i]) {
                slots.add(ITEMS_START + gridRows[i] * 9 + gridCols[i]);
            } else {
                slots.add(-1);
            }
        }
        return slots;
    }

    private static void placeItem(final int[] gridRows, final int[] gridCols, final boolean[] placed,
            final int idx, final int row, final int col) {
        gridRows[idx] = row;
        gridCols[idx] = col;
        placed[idx] = true;
    }

    private static void centerGrid(final int[] gridRows, final int[] gridCols, final boolean[] placed,
            final boolean[] absolute, final boolean vertical) {
        if (vertical) {
            for (int c = 0; c < 9; c++) {
                int minRow = Integer.MAX_VALUE;
                int maxRow = Integer.MIN_VALUE;
                boolean hasAbsolute = false;
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridCols[i] == c) {
                        hasAbsolute |= absolute[i];
                        minRow = Math.min(minRow, gridRows[i]);
                        maxRow = Math.max(maxRow, gridRows[i]);
                    }
                }
                if (maxRow == Integer.MIN_VALUE || hasAbsolute) {
                    continue;
                }
                int offset = (4 - (maxRow - minRow + 1)) / 2 - minRow;
                if (offset == 0) {
                    continue;
                }
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridCols[i] == c) {
                        gridRows[i] += offset;
                    }
                }
            }
        } else {
            for (int r = 0; r < 4; r++) {
                int minCol = Integer.MAX_VALUE;
                int maxCol = Integer.MIN_VALUE;
                boolean hasAbsolute = false;
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridRows[i] == r) {
                        hasAbsolute |= absolute[i];
                        minCol = Math.min(minCol, gridCols[i]);
                        maxCol = Math.max(maxCol, gridCols[i]);
                    }
                }
                if (maxCol == Integer.MIN_VALUE || hasAbsolute) {
                    continue;
                }
                int offset = (9 - (maxCol - minCol + 1)) / 2 - minCol;
                if (offset == 0) {
                    continue;
                }
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridRows[i] == r) {
                        gridCols[i] += offset;
                    }
                }
            }
        }
    }
}