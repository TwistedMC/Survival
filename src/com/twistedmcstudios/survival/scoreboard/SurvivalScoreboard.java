package com.twistedmcstudios.survival.scoreboard;

import com.twistedmcstudios.core.common.bossbar.animations.ScrollPauseAnimation;
import com.twistedmcstudios.core.scoreboard.WritableTwistedMCScoreboard;
import org.bukkit.scoreboard.Scoreboard;

public class SurvivalScoreboard extends WritableTwistedMCScoreboard {
	private int _shineIndex = 0;
	private String[] title;

	public void updateTitle() {
		this.title = new ScrollPauseAnimation("   " + "SURVIVAL" + "   ")
				.withPrimaryColor("&#3498DB")
				.withSecondaryColor("&f")
				.withTertiaryColor("&b")
				.bold()
				.build();

		setSidebarName(this.title[_shineIndex]);

		if (++_shineIndex == this.title.length) {
			_shineIndex = 0;
		}
	}

	@Override
	public void draw() {
		if (_bufferedLines.size() > 15) {
			while (_bufferedLines.size() > 15) {
				_bufferedLines.remove(_bufferedLines.size() - 1);
			}
		}
		super.draw();
	}

	public Scoreboard getScoreboard() {
		return getHandle();
	}
}
