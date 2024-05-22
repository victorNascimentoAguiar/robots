package sentinel;

import robocode.*;
import java.awt.geom.Point2D;
import robocode.util.Utils;

public class SmoothOperator extends AdvancedRobot {

    private double moveDirection = 1; // Direção do movimento

    @Override
    public void run() {
        setAdjustGunForRobotTurn(true); // Permite que o canhão gire independentemente do corpo
        setAdjustRadarForGunTurn(true); // Permite que o radar gire independentemente do canhão

        while (true) {
            if (getRadarTurnRemaining() == 0.0)
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY); // Gire o radar continuamente
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        double enemyAbsoluteBearing = getHeadingRadians() + event.getBearingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()));

        double enemyDistance = event.getDistance();
        double enemyVelocity = event.getVelocity();

        // Implementação da estratégia de movimento oscilatório
        setTurnRight(event.getBearing() + 90 - 30 * moveDirection);

        // Se o robô estiver perto demais do inimigo, recue
        if (enemyDistance < 100 && getOthers() > 2) {
            moveDirection = -1;
        } else if (enemyDistance > 200 || getOthers() <= 2) {
            moveDirection = 1;
        }

        // Verifique se o robô está muito perto da parede
        if (getX() < 50 || getY() < 50 || getX() > getBattleFieldWidth() - 50 || getY() > getBattleFieldHeight() - 50) {
            moveDirection *= -1;
        }

        setAhead(100 * moveDirection);

        // Aumente a potência do tiro quando o inimigo estiver mais próximo
        double bulletPower = Math.min(3.0, getEnergy() - .1);
        if (enemyDistance < 100) {
            bulletPower = Math.min(3.0, getEnergy() - .1);
        } else if (enemyDistance < 200) {
            bulletPower = Math.min(2.0, getEnergy() - .1);
        } else {
            bulletPower = Math.min(1.0, getEnergy() - .1);
        }

        // Implementação da estratégia de direcionamento de tiros usando regras fuzzy
        double guessFactor;
        if (enemyVelocity != 0) {
            if (Math.sin(event.getHeadingRadians() - enemyAbsoluteBearing) * enemyVelocity < 0) {
                guessFactor = (enemyVelocity < 0 ? -1 : 1);
            } else {
                guessFactor = (Math.abs(enemyVelocity) > 8 ? 1 : 2);
            }
        } else {
            guessFactor = 1;
        }

        aimAndFire(event, bulletPower, guessFactor);  // Certifique-se de que você está chamando o método aimAndFire aqui
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        moveDirection *= -1; // Mude a direção do movimento ao ser atingido por uma bala
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        double bearing = event.getBearing();
        if (bearing > -90 && bearing <= 90) {
            setBack(100);
        } else {
            setAhead(100);
        }
        setTurnRight(45);
        execute();
    }

    private void aimAndFire(ScannedRobotEvent event, double bulletPower, double guessFactor) {
        double enemyBearing = event.getBearing();
        double enemyHeading = event.getHeading();
        double enemyVelocity = event.getVelocity();

        double deltaTime = 0;
        double battleFieldHeight = getBattleFieldHeight(), 
               battleFieldWidth = getBattleFieldWidth();
        double predictedX = getX() + Math.sin(Math.toRadians(getHeading() + enemyBearing)) * event.getDistance(), 
               predictedY = getY() + Math.cos(Math.toRadians(getHeading() + enemyBearing)) * event.getDistance();
        while((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(getX(), getY(), predictedX, predictedY)){        
            predictedX += Math.sin(Math.toRadians(enemyHeading)) * enemyVelocity * guessFactor;    
            predictedY += Math.cos(Math.toRadians(enemyHeading)) * enemyVelocity * guessFactor;
            if( predictedX < 18.0 
                || predictedY < 18.0
                || predictedX > battleFieldWidth - 18.0
                || predictedY > battleFieldHeight - 18.0){
                predictedX = Math.min(Math.max(18.0, predictedX), 
                    battleFieldWidth - 18.0);    
                predictedY = Math.min(Math.max(18.0, predictedY), 
                    battleFieldHeight - 18.0);
                break;
            }
        }
        double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
        setTurnRadarRightRadians(Utils.normalRelativeAngle(Math.atan2(predictedX - getX(), predictedY - getY()) - getRadarHeadingRadians()));
        setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
        fire(bulletPower);
    }
}
