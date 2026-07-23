package io.github.smokahs.solpotpie.book;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.ICustomComponent;

import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StatsComponent implements ICustomComponent {
	int width = 116;
	@SerializedName("line_height") int lineHeight = 11;

	private transient int x;
	private transient int y;
	private transient List<BookStats.Row> rows = new ArrayList<>();

	@Override
	public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {}

	@Override
	public void build(int componentX, int componentY, int pageNum) {
		x = componentX;
		y = componentY;
	}

	@Override
	public void onDisplayed(IComponentRenderContext context) {
		rows = BookStats.rows();
	}

	@Override
	public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
		if (rows.isEmpty()) {
			rows = BookStats.rows();
		}

		BookStats.drawRows(graphics, context, rows, x, y, width, lineHeight, mouseX, mouseY);
	}
}
