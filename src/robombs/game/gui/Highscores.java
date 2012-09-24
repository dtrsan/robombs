package robombs.game.gui;

import robombs.game.model.*;
import java.util.*;

/**
 * An extended table for displaying the current scores. Highscores is a stupid
 * name for it...anyway...:-)
 */
public class Highscores extends Table {

	private List<String> names = new ArrayList<String>();
	private List<Integer> teams = new ArrayList<Integer>();

	/**
	 * Create a new instance. A new instance will be created everytime that the
	 * score changes. But it's a quite lightweight component, so this shouldn't
	 * be a problem.
	 * 
	 * @param hi
	 *            the HighscoreTable, i.e. the "model" that holds the actual
	 *            data.
	 */
	public Highscores(HighscoreTable hi) {
		super("Highscores", hi.getLineCount() + 1 + (hi.getTeamCount() != 0 ? (hi.getTeamCount() + 2) : 0), 3, 10, 34, 320, 304);
		this.setColumnSize(0, 150);
		this.setColumnSize(1, 60);
		this.setColumnSize(2, 60);
		this.setRowSize(0, 20);
		this.setCell(0, 0, "Player");
		this.setCell(0, 1, "Wins");
		this.setCell(0, 2, "Loses");
		names.clear();
		teams.clear();
		List<TeamEntry> teamEntries = new ArrayList<TeamEntry>();
		for (int i = 0; i < hi.getLineCount(); i++) {
			String player = hi.getPlayerName(i);
			int wins = hi.getWins(i);
			int lost = hi.getLoses(i);
			int team = hi.getTeam(i);
			
			if (team != 0) {
				player = team + " - " + player;
			}
			
			this.setRowSize(i + 1, 16);
			this.setCell(i + 1, 0, player);
			this.setCell(i + 1, 1, Integer.valueOf(wins));
			this.setCell(i + 1, 2, Integer.valueOf(lost));
			names.add(hi.getPlayerName(i));
			teams.add(team);

			if (team != 0) {
				boolean found = false;
				for (TeamEntry te : teamEntries) {
					if (te.team == team) {
						found = true;
						break;
					}
				}
				if (!found) {
					teamEntries.add(new TeamEntry(team, wins, lost));
				}
			}
		}
		Collections.sort(teamEntries);

		int teamCount = hi.getTeamCount();
		int start = hi.getLineCount() + 2;
		if (teamCount > 0) {
			for (int i = start - 1; i < start + 2; i++) {
				this.setRowSize(i, 16);
			}

			this.setCell(start, 0, "Team ranking");
			this.setCell(start, 1, "Wins");
			this.setCell(start, 2, "Loses");
			int i = 0;
			for (TeamEntry te : teamEntries) {
				int row = start + i + 1;
				this.setCell(row, 0, "Team " + te.team);
				this.setCell(row, 1, te.wins);
				this.setCell(row, 2, te.loses);
				this.setRowSize(row, 16);
				i++;
			}
		}

		// Collections.sort(names);

		boolean swapped = false;
		do {
			swapped = false;
			for (int i = 0; i < names.size() - 1; i++) {
				String name1 = names.get(i);
				String name2 = names.get(i + 1);
				if (name1.compareTo(name2) > 0) {
					names.set(i, name2);
					names.set(i + 1, name1);

					Integer t1 = teams.get(i);
					teams.set(i, teams.get(i + 1));
					teams.set(i + 1, t1);
					swapped = true;
				}
			}
		} while (swapped);
	}

	public List<String> getPlayerNames() {
		return names;
	}

	public List<Integer> getTeams() {
		return teams;
	}

	private static class TeamEntry implements Comparable<TeamEntry> {

		private int team;
		private int wins;
		private int loses;

		public TeamEntry(int team, int wins, int loses) {
			this.team = team;
			this.wins = wins;
			this.loses = loses;
		}

		@Override
		public int compareTo(TeamEntry o) {
			int d = o.wins - wins;
			if (d == 0) {
				d = loses - o.loses;
			}
			if (d == 0) {
				d = team - o.team;
			}
			return d;
		}

	}

}
