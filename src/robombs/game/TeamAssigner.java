package robombs.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import robombs.clientserver.NetLogger;
import robombs.game.model.LocalPlayerObject;
import robombs.game.model.PlayerInfo;

/**
 * Static dumpster class for team assignments. Team support has been added years
 * after the game was written and while it works pretty well, it's
 * implementation is kinda...weird...
 * 
 * @author EgonOlsen
 * 
 */
public class TeamAssigner {

	private static Map<Integer, Map<Integer, Integer>> assignments = new HashMap<Integer, Map<Integer, Integer>>();

	public static synchronized int rotateTeam(int cId, int oId) {
		int team = getTeam(cId, oId);
		team++;
		if (team >= 5) {
			team = 0;
		}
		assignTeam(cId, oId, team);
		return team;
	}

	public static synchronized void assignTeam(int cId, int oId, int team) {
		Map<Integer, Integer> teams = assignments.get(cId);
		if (teams == null) {
			teams = new HashMap<Integer, Integer>();
			assignments.put(cId, teams);
		}
		// Logger.log("Assigned " + cId + "/" + oId + " to team " + team);
		teams.put(oId, team);
	}

	public static int getTeam(PlayerInfo pi) {
		return getTeam(pi.getClientID(), pi.getObjectID());
	}

	public static int getTeam(LocalPlayerObject lpo) {
		return getTeam(lpo.getClientID(), lpo.getObjectID());
	}

	public static synchronized int getTeam(int cId, int oId) {
		Map<Integer, Integer> teams = assignments.get(cId);
		if (teams != null) {
			Integer team = teams.get(oId);
			if (team != null) {
				return team.intValue();
			}
		}
		return 0;
	}

	public static synchronized void removeTeamAssigment(int cId, int oId) {
		Map<Integer, Integer> rem = assignments.get(cId);
		if (rem != null) {
			Integer t = rem.remove(oId);
			if (t != null) {
				NetLogger.log("Removed team assignment for " + cId + "/" + oId);
				if (rem.isEmpty()) {
					assignments.remove(cId);
				}
			}
		}
	}

	public static void clear() {
		assignments.clear();
		NetLogger.log("Cleared team assignments!");
	}

	public static synchronized List<int[]> getAssignments() {
		List<int[]> res = new ArrayList<int[]>();
		Set<Integer> keys = assignments.keySet();
		for (Integer cId : keys) {
			Set<Integer> keys2 = assignments.get(cId).keySet();
			for (Integer oId : keys2) {
				int[] part = new int[3];
				Integer team = assignments.get(cId).get(oId);
				part[0] = cId;
				part[1] = oId;
				part[2] = team;
				res.add(part);
			}
		}
		return res;
	}

}
