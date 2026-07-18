package io.github.smokahs.solpotpie.client.gui;

import io.github.smokahs.solpotpie.client.gui.elements.UIBox;
import io.github.smokahs.solpotpie.client.gui.elements.UILabel;
import io.github.smokahs.solpotpie.tracking.HeartsHandler;

import java.awt.*;

import static io.github.smokahs.solpotpie.lib.Localization.localized;

public class StatsPage extends Page {
	StatsPage(double lifetimePoints, int recentVariety, Rectangle frame) {
		super(frame, localized("gui", "food_book.stats"));

		int hearts = HeartsHandler.heartsFromPoints(lifetimePoints);
		double nextHeartAt = HeartsHandler.cumulativeCost(hearts + 1);

		// dummy box to center the display
		mainStack.addChild(new UIBox(new Rectangle(0, 0, 1, 12), new Color(0, 0, 0, 0)));

		mainStack.addChild(statWithIcon(
				icon(FoodBookScreen.carrotImage),
				String.format("%d", hearts),
				localized("gui", "food_book.stats.hearts")
		));

		mainStack.addChild(statWithIcon(
				icon(FoodBookScreen.carrotImage),
				String.format("%.1f", lifetimePoints),
				localized("gui", "food_book.stats.lifetime_points")
		));

		UILabel nextHeartLabel = new UILabel(
				localized("gui", "food_book.stats.next_heart", nextHeartAt - lifetimePoints));
		nextHeartLabel.color = FoodBookScreen.lessBlack;
		nextHeartLabel.tooltip = localized("gui", "food_book.stats.next_heart_tooltip");
		mainStack.addChild(nextHeartLabel);

		mainStack.addChild(makeSeparatorLine());

		mainStack.addChild(statWithIcon(
				icon(FoodBookScreen.carrotImage),
				String.format("%d", recentVariety),
				localized("gui", "food_book.stats.recent_variety")
		));

		updateMainStack();
	}
}
