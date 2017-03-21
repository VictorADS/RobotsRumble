/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/RandomFire.java 2014-10-28 buixuan.
 * ******************************************************/
package Enemy;

import robotsimulator.Brain;
import characteristics.IRadarResult;
import characteristics.Parameters;
import characteristics.Parameters.Direction;
import characteristics.IFrontSensorResult;
import characteristics.IFrontSensorResult.Types;

import java.util.ArrayList;
import java.util.Random;

import javax.jws.soap.SOAPBinding.ParameterStyle;

@SuppressWarnings("Duplicates")
public class Viking extends Brain {
    /**
     * VS CampFire : 7 points
     * VS RandomFire : 13 points
     * VS BootingBerzerk : 10 points
     * VS HighwayFugitive : 10 points
     */
    //---PARAMETERS---//
    private static final double HEADINGPRECISION = 0.001;
    private static double posEnnemy=0.0;

    //---VARIABLES---//
    private boolean turnTask,fireTask,moveTask;
    private boolean detectEnnemy,detectBullet,detectTeam, endTask;
    private double distanceBullet=0.0;
    private double directionBullet=0.0;
    private double distanceEnnemy=0.0;
    private double directionEnnemy=0.0;
    private int countStepFindTeam = 0;
    private int turnWall = 0;
    private int turnNormal = 0;
    private int fireCounter = 0;
    private int NbStep = -1;
    private int bulletDetected;
    private static int id = 0;
    private int whoAmI=0;
    private int limiteTerrain=500;
    private boolean testWall = false;
    private int nombreTourTirQuiSertARien = 0;
    private int tourJeQuitte = 350;
    private IRadarResult rbOld;
    //---CONSTRUCTORS---//
    public Viking() { super(); }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        whoAmI = id++;
        moveTask=true;
        fireTask=false;
        detectEnnemy=false;
        detectBullet=false;
        detectTeam=false;
        endTask=false;
        //firstMove=true;
    }

    public void step() {
        posEnnemy = 0.0;
        NbStep++;
        ArrayList<IRadarResult> radarResults = detectRadar();
        IRadarResult rb = Tools.containsEnemies(radarResults);
        if(whoAmI>2){

            if(NbStep%15==0){
                stepTurn(Direction.LEFT);
                return ;
            }

            if(detectFront().getObjectType() == Types.TeamMainBot || detectFront().getObjectType() == Types.TeamSecondaryBot){
                Random r = new Random();
                turnNormal = r.nextInt(19)+2;
            }

            if((detectFront().getObjectType() == Types.WALL || detectFront().getObjectType() == Types.Wreck) /*&& !testWall*/) {
                Random r = new Random();
                turnWall = r.nextInt(101)+150;
                //	testWall = true;
            }

            if(turnWall > 0) {
                turnNormal = 0;
                if(turnWall%5 == 0 || turnWall%5 == 1)
                    stepTurn(Direction.LEFT);
                else
                    moveBack();
                turnWall--;
                return;
            }
			/*	if(turnWall == 0)
				testWall=false;*/

            if(turnNormal > 0){
                turnWall = 0;
                if(turnNormal%5 == 0 || turnNormal%5==1)
                    stepTurn(Direction.LEFT);
                else
                    moveBack();

                turnNormal--;
                return;
            }
            move();
        }

        if(rbOld != null && fireCounter > 0) {
            fireCounter--;
            if (rbOld.getObjectType() != IRadarResult.Types.Wreck &&
                    (rbOld.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                            rbOld.getObjectType() == IRadarResult.Types.OpponentSecondaryBot)) {
                fire(rbOld.getObjectDirection());
                return;
            }
        }

		/*if(Tools.containsEnemiesWreck(radarResults)){
			if(NbStep%200 ==0){
				moveBack();
				return;
			}
		}*/
        if(rb != null) {
            rbOld = rb;
            fireCounter = 700;
            return;
            /*
            if(rb.getObjectType() == IRadarResult.Types.Wreck || rb.getObjectType() == IRadarResult.Types.OpponentMainBot ){
                System.out.println("le nombre de tour qui sert à rien = "+nombreTourTirQuiSertARien);
                if(nombreTourTirQuiSertARien > 800){
                    if(tourJeQuitte >= 0){
                        System.out.println("je move back la con de toi");
                        if(tourJeQuitte%10 == 0 || tourJeQuitte%10 == 1 )
                            stepTurn(Direction.LEFT);
                        else
                            move();
                        tourJeQuitte--;
                        return;
                    }
                    else{
                        nombreTourTirQuiSertARien=0;
                        tourJeQuitte = 350;
                    }
                }
                else
                    nombreTourTirQuiSertARien++;
            }
            fire(rb.getObjectDirection());
            return;
            */
        }
        tourJeQuitte = 350;
        nombreTourTirQuiSertARien=0;

        for(IRadarResult r : radarResults){
            if(posEnnemy != 0.0){
                fire(posEnnemy);
            }
            if(detectBullet){
                /*
                System.out.println("bullet found");
                if(distanceBullet <= Parameters.bulletRange){
                    if(directionBullet>-1 && directionBullet< 0)
                        stepTurn(Parameters.Direction.RIGHT);
                    if(directionBullet>0 && directionBullet<1)
                        stepTurn(Parameters.Direction.LEFT);
                }
                detectBullet=false;
                fireTask = true;
                */
            }
            else if(fireTask){
                //	fire(directionBullet);
                fireTask=false;
                detectBullet=false;
                move();
                return;
            }
            else if(detectEnnemy){
                fire(directionEnnemy);
                nombreTourTirQuiSertARien++;
                detectEnnemy=false;
                fireTask=false;
            }
            else if(moveTask){
                if(r.getObjectType()==IRadarResult.Types.BULLET && bulletDetected <= 0){
                    bulletDetected = 100;
                    distanceBullet = r.getObjectDistance();
                    directionBullet = r.getObjectDirection();
                    detectBullet=true;
                    turnTask=true;
                    countStepFindTeam=0;
                    move();
                    return;
                }
                else if(r.getObjectType()==IRadarResult.Types.OpponentMainBot || r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot){
                    bulletDetected--;
                    distanceEnnemy = r.getObjectDistance();
                    directionEnnemy = r.getObjectDirection();
                    fireTask=true;
                    detectEnnemy=true;
                    countStepFindTeam=0;
                    move();
                    return;
                }
                else if(r.getObjectType()==IRadarResult.Types.TeamSecondaryBot || r.getObjectType()==IRadarResult.Types.TeamMainBot){
                    if(countStepFindTeam>5){
                        detectTeam = false;
                    }
                    else{
                        countStepFindTeam++;
                        detectTeam=true;
                    }
                    if(fireTask && !detectEnnemy)
                        fireTask=false;
                    move();
                }
                else if(detectFront().getObjectType() == IFrontSensorResult.Types.WALL){
                    stepTurn(Parameters.Direction.RIGHT);
                }
                else {
                    endTask = true;
                    moveTask = false;
                }
            }
            else if(endTask) {
                endTask = false;
                moveTask=true;
                if(NbStep%2==0)
                    move();
                else
                    stepTurn(Direction.LEFT);
                return;
            }
        }

        if(detectFront().getObjectType() == Types.TeamMainBot || detectFront().getObjectType() == Types.TeamSecondaryBot){
            //	Random r = new Random();
            //	turnWall = r.nextInt(19)+2;
            turnWall = 2;
        }

        if((detectFront().getObjectType() == Types.WALL || detectFront().getObjectType() == Types.Wreck) /*&& !testWall*/) {
            Random r = new Random();
            turnWall = r.nextInt(101)+50;
            //	testWall = true;
        }

        if(turnWall > 0) {
            turnNormal = 0;
            if(turnWall%5 == 0 || turnWall%5 == 1)
                stepTurn(Direction.LEFT);
            else
                moveBack();
            turnWall--;
            return;
        }
		/*	if(turnWall == 0)
			testWall=false;*/

        if(turnNormal > 0){
            turnWall = 0;
            if(turnNormal%5 == 0 || turnNormal%5==1)
                stepTurn(Direction.LEFT);
            else
                moveBack();

            turnNormal--;
            return;
        }
        move();
    }

    private boolean isHeading(double dir){
        return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamAMainBotStepTurnAngle;
    }
}
