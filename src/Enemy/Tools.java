package Enemy;

import characteristics.IRadarResult;
import robotsimulator.Brain;

import java.util.ArrayList;

/**
 * Created by Shaficks on 21/02/2017.
 */
public class Tools {

    public static IRadarResult containsEnemies(ArrayList<IRadarResult> results) {
        IRadarResult result = null;
        for(IRadarResult i : results) {
            if(i.getObjectType() == characteristics.IRadarResult.Types.OpponentSecondaryBot ||
                    i.getObjectType() == characteristics.IRadarResult.Types.OpponentMainBot) {

                    result = i;
            }
        }
        return result;
    }

    public static boolean containsEnemiesWreck(ArrayList<IRadarResult> results) {
        for(IRadarResult r : results) {
            if(r.getObjectType() == IRadarResult.Types.Wreck) {
                return true;
            }
        }
        return false;
    }

    public static IRadarResult containsTeamMembers(ArrayList<IRadarResult> results) {
        for(IRadarResult i : results) {
            if(i.getObjectType() == IRadarResult.Types.TeamMainBot ||
                    i.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                return i;
            }
        }
        return null;
    }

    public static boolean obstacleDevant(ArrayList<IRadarResult> results, Brain brain) {
        for(IRadarResult i : results) {
            if(brain.getHeading() >= i.getObjectDirection()-0.5 &&
                    brain.getHeading() <= i.getObjectDirection()+0.5) {
                return true;
            }
        }
        return false;
    }

}
