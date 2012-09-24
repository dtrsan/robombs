package robombs.game.model;

import java.util.*;

import robombs.clientserver.*;
import robombs.game.*;

/**
 * Contains the data of the in game score table. This is created from the score
 * data that the server transmit each time the score changes.
 */
public class HighscoreTable {

	private List<InfoLine> table = new ArrayList<InfoLine>();

	public HighscoreTable() {
	}

	/**
	 * Clears the table
	 */
	public void clear() {
		table.clear();
	}

	/**
	 * Adds a new entry to the table. This is not a complete line but one part
	 * of it.
	 * 
	 * @param il
	 *            the entry
	 */
	public void addLineEntry(InfoLine il) {
		/*
		 * for (InfoLine io:table) { if (io.getCount()==il.getCount() &&
		 * io.getKey().equals(il.getKey())) { // We already have this entry
		 * return; } }
		 */
		table.add(il);

	}

	/**
	 * Adds a complete line to the table, i.e. the player's name, the number of
	 * frags he/she has and the number of deaths.
	 * 
	 * @param pi
	 *            the PlayerInfo for that player
	 */
	public void addLine(PlayerInfo pi) {
		int pos = table.size();
		int team = TeamAssigner.getTeam(pi);
		String name = pi.getName();
		InfoLine il = new InfoLine(InfoLine.SCORE_TABLE, pos, "name", name);
		InfoLine il2 = new InfoLine(InfoLine.SCORE_TABLE, pos, "wins", String.valueOf(pi.getWins()));
		InfoLine il3 = new InfoLine(InfoLine.SCORE_TABLE, pos, "loses", String.valueOf(pi.getLoses()));
		InfoLine il4 = new InfoLine(InfoLine.SCORE_TABLE, pos, "team", String.valueOf(team));
		table.add(il);
		table.add(il2);
		table.add(il3);
		table.add(il4);
	}

	public int getTeamCount() {
		boolean[] teams = new boolean[5];
		for (InfoLine line : table) {
			if (line.getKey().equals("team")) {
				int team = Integer.parseInt(line.getValue());
				if (team != 0) {
					teams[team] = true;
				}
			}
		}

		int cnt = 0;
		for (boolean bo : teams) {
			if (bo) {
				cnt++;
			}
		}
		return cnt;
	}

	/**
	 * Returns the number of lines in the table.
	 * 
	 * @return int the number of lines
	 */
	public int getLineCount() {
		return table.size() / 4;
	}

	/**
	 * Gets the name of the player in i-th line.
	 * 
	 * @param i
	 *            the line number
	 * @return String the name
	 */
	public String getPlayerName(int i) {
		return ((InfoLine) table.get(i * 4)).getValue();
	}

	/**
	 * Gets the number of wins in i-th line.
	 * 
	 * @param i
	 *            the line number
	 * @return int the number
	 */
	public int getWins(int i) {
		return Integer.parseInt(((InfoLine) table.get(i * 4 + 1)).getValue());
	}

	/**
	 * Gets the number of loses in i-th line.
	 * 
	 * @param i
	 *            the line number
	 * @return int the number
	 */
	public int getLoses(int i) {
		return Integer.parseInt(((InfoLine) table.get(i * 4 + 2)).getValue());
	}

	public int getTeam(int i) {
		return Integer.parseInt(((InfoLine) table.get(i * 4 + 3)).getValue());
	}

	/**
	 * Creates a DataContainer from the table's content that can be transfered
	 * via the network.
	 * 
	 * @return DataContainer the container with the table's data
	 */
	public DataContainer getContainer() {
		InfoDataContainer idc = new InfoDataContainer();
		for (Iterator<InfoLine> itty = table.iterator(); itty.hasNext();) {
			InfoLine il = itty.next();
			idc.add(il);
		}
		return idc;
	}

	/**
	 * Adds the table's content to an existing container. This container isn't a
	 * generic DataContainer any longer, but a specific InfoDataContainer
	 * 
	 * @param dc
	 *            the container
	 */
	public void addToContainer(InfoDataContainer dc) {
		for (Iterator<InfoLine> itty = table.iterator(); itty.hasNext();) {
			InfoLine il = itty.next();
			dc.add(il);
		}
	}
}
