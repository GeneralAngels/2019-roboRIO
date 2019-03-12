package frc.robot.bobot.drive;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.SpeedController;
import frc.robot.bobot.Subsystem;
import frc.robot.bobot.control.PID;
import org.json.JSONObject;

public class DifferentialDrive<T extends SpeedController> extends Subsystem {

    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String VELOCITY = "v";
    public static final String OMEGA = "w";
    public static final String GYRO = "gyro";
    public static final String ODOMETRY = "odometry";
    public double Lsetpoint = 0;
    public double Rsetpoint = 0;
    public PID motorControlLeft;
    public PID motorControlRight;
    public PID motorControlLeftP;
    public PID motorControlRightP;
    public AHRS gyro;
    public double WHEEL_DISTANCE = 0.51;
    public double WHEEL_RADIUS = 0.1016;
    public double MAX_V = 1;
    //    for good results max omega = max_v * 2 /wheel_distance
//    MAX_V * 2 / WHEEL_DISTANCE
    public double MAX_OMEGA = 3.14 * 2;
    public double[] realVOmega;
    public double gearRatio = 14.0 / 60.0;
    public double ENCODER_COUNT_PER_REVOLUTION = 512;
    public double ENCODER_TO_RADIAN = (Math.PI * 2) / (4 * ENCODER_COUNT_PER_REVOLUTION);
    public double setPointVPrev = 0;
    public double setPointOmegaPrev = 0;
    public double thetaRobotPrev = 0;
    public double x = 0;
    public double y = 0;
    public double rightEncPrev = 0;
    public double leftEncPrev = 0;
    public double MAX_WHEEL_VELOCITY = 25;
    public double lastSetPointsR = 0;
    public double lastSetPointsL = 0;
    public double lastav = 0;
    public double leftMeters;
    public double rightMeters;
    public double outputLeft = 0;
    public double outputRight = 0;
    public double[] VOmegaReal = {0, 0};
    public double[] VOmega = {0, 0};
    public double Vleft = 0;
    public double Vright = 0;
    public double leftMetersPrev = 0;
    public double rightMetersPrev = 0;
    public double[] encoderMeters = {0, 0};
    public double[] encoders = {0, 0};
    public double offsetGyro = 0;
    public double distance = 0;
    public double theta = 0;
    public double motorOutputLeft = 0;
    public double motorOutputRight = 0;
    public boolean check = true;
    public double currentMetersRight = 0;
    public double currentMetersLeft = 0;
    protected Drivebox<T> left = new Drivebox<>(), right = new Drivebox<>();
    protected Odometry odometry = new Odometry();
    double motorOutputLeftPrev = 0;
    double motorOutputRightPrev = 0;


    public DifferentialDrive() {
        motorControlLeft = new PID();
        motorControlRight = new PID();
        motorControlLeftP = new PID();
        motorControlRightP = new PID();
        motorControlLeft.setPIDF(0, 0.2, 0, 0.45);
        motorControlRight.setPIDF(0, 0.2, 0, 0.44);
        motorControlLeftP.setPIDF(2.7, 0.4, 0.2, 0);
        motorControlRightP.setPIDF(2.7, 0.4, 0.35, 0);
//        robotA
//        motorControlLeft.setPIDF(0, 0.1, 0, 0.39);
//        motorControlRight.setPIDF(0, 0.1, 0, 0.4);
//        motorControlLeftP.setPIDF(0, 0.1, 0, 0.39);
//        motorControlRightP.setPIDF(0, 0.1, 0, 0.4);

//        leftMeters = (left.getEncoder().getRaw()*ENCODER_TO_RADIAN)/(2*Math.PI*WHEEL_RADIUS);
//        rightMeters = (right.getEncoder().getRaw()*ENCODER_TO_RADIAN)/(2*Math.PI*WHEEL_RADIUS);

    }

    public static double noPIDCalculateRight(double speed, double turn) {
        if (speed == 0) {
            return turn;
        } else {
            return (speed + turn);
        }
    }

    public static double noPIDCalculateLeft(double speed, double turn) {
        if (speed == 0) {
            return turn;
        } else {
            return (turn - speed);
        }
    }

    public void setAutonomous(double v, double w) {
        // If limiting is needed
        // set(v/MAX_V,w/MAX_OMEGA);
        // If not
        set(v, w);
    }

    public void setStickNoPID(double speed, double turn) {
        double l, r;
        l = noPIDCalculateLeft(speed, turn);
        r = noPIDCalculateRight(speed, turn);
//        log("L "+l+" R "+r);
        direct(l, r);
    }

    public void setTank(double Vl, double Vr) {
//        double[] speeds = accControl(Vl*MAX_WHEEL_VELOCITY,Vr*MAX_WHEEL_VELOCITY);
//        Vl = speeds[0];
//        Vr = speeds[1];

        Vl = Vl * MAX_WHEEL_VELOCITY;
        Vr = Vr * MAX_WHEEL_VELOCITY;
        Vleft = Vl;
        Vright = Vr;
        VOmega = wheelsToRobot(Vleft, Vright);
        encoders[0] = left.getEncoder().getRaw();
        encoders[1] = right.getEncoder().getRaw();
        double encoderLeft = encoders[0] * ENCODER_TO_RADIAN * gearRatio;
        double encoderRight = encoders[1] * ENCODER_TO_RADIAN * gearRatio;
        double motorOutputLeft = motorControlLeft.pidVelocity(encoderLeft, Vl);
        double motorOutputRight = motorControlRight.pidVelocity(encoderRight, Vr);
//        log("encoderL: "+encoderLeft+" encoderR: "+encoderRight);
        if (Math.abs(motorOutputLeft) < 0.1)
            motorOutputLeft = 0;
        if (Math.abs(motorOutputRight) < 0.1)
            motorOutputRight = 0;
//        motorOutputLeft = (0.8*motorOutputLeft) + (0.2*motorOutputLeftPrev);
//        motorOutputRight = (0.8*motorOutputRight) + (0.2*motorOutputRightPrev);
        //log("left: " + Vl + " right: " + Vr);
        motorOutputLeftPrev = motorOutputLeft;
        motorOutputRightPrev = motorOutputRight;

        direct(motorOutputLeft / 12, motorOutputRight / 12);
        updateOdometry();
    }

    public void set(double speed, double turn) {
        VOmega[0] = speed;
        VOmega[1] = turn;
        double[] V = robotToWheels(VOmega[0], VOmega[1]);
        double setpointV = VOmega[0];
        double setpointOmega = VOmega[1];
//        setpointV = (setpointV * 0.5) + (setPointVPrev * 0.5);
//        setpointOmega = (setpointOmega * 0.5) + (setPointOmegaPrev * 0.5);
        if (Math.abs(setpointV) < 0.2) setpointV = 0;
        if (Math.abs(setpointOmega) < 0.2) setpointOmega = 0;
//        double[] motorOutput = calculateOutputs(setpointV * MAX_V, setpointOmega * MAX_OMEGA);
        encoders[0] = left.getEncoder().getRaw();
        encoders[1] = right.getEncoder().getRaw();
        double encoderLeft = encoders[0] * ENCODER_TO_RADIAN * gearRatio;
        double encoderRight = encoders[1] * ENCODER_TO_RADIAN * gearRatio;
        double motorOutputLeft = motorControlLeft.pidVelocity(encoderLeft, V[0]);
        double motorOutputRight = motorControlRight.pidVelocity(encoderRight, V[1]);
        log("left: " + motorOutputLeft / 12.0);
        log("right: " + motorOutputRight / 12.0);
        if (Math.abs(motorOutputLeft) < 0.1)
            motorOutputLeft = 0;
        if (Math.abs(motorOutputRight) < 0.1)
            motorOutputRight = 0;
        motorOutputLeftPrev = motorOutputLeft;
        motorOutputRightPrev = motorOutputRight;
        setPointVPrev = setpointV;
        setPointOmegaPrev = setpointOmega;
        direct(motorOutputLeft / 12.0, motorOutputRight / 12.0);
        updateOdometry();

//        leftMeters = (encoders[0] / (ENCODER_COUNT_PER_REVOLUTION * 4) * (60.0 / 14.0)) * (2 * Math.PI * WHEEL_RADIUS);
//        rightMeters = (encoders[1] / (ENCODER_COUNT_PER_REVOLUTION * 4) * (60.0 / 14.0)) * (2 * Math.PI * WHEEL_RADIUS);
//        encoderMeters[0] = (leftMeters - leftMetersPrev) / 0.02;
//        encoderMeters[1] = (rightMeters - rightMetersPrev) / 0.02;
//        VOmegaReal = wheelsToRobot(encoderMeters[0], encoderMeters[1]);
////        log("Il:" + motorControlLeft.getIntegral() + " Ir:" + motorControlRight.getIntegral() + " Vr=" + realVOmega[0] + " Left=" + motorControlLeft.getDerivative() + " Right=" + motorControlRight.getDerivative());
//
//        setPointVPrev = setpointV;
//        setPointOmegaPrev = setpointOmega;
//        leftMetersPrev = leftMeters;
//        rightMetersPrev = rightMeters;
////        log("left: " + motorOutput[0] + " right: " + motorOutput[1]);
//        direct(motorOutput[0] / 12, motorOutput[1] / 12);
//        updateOdometry();
    }

    public double[] accControl(double VL, double VR) {
        double maxAcc = 0.3; //   12/(m/s^2)
        double av = (VL + VR) / 2;
        double error = av - this.lastav;
        if (error > maxAcc) {
            this.lastSetPointsR = this.lastSetPointsR + 0.15;
            this.lastSetPointsL = this.lastSetPointsL + 0.15;
            return new double[]{this.lastSetPointsL, this.lastSetPointsR};
        } else if (error < -maxAcc) {
            this.lastSetPointsR = this.lastSetPointsR - 0.15;
            this.lastSetPointsL = this.lastSetPointsL - 0.15;
            return new double[]{this.lastSetPointsL, this.lastSetPointsR};
        }
        this.lastSetPointsL = VL;
        this.lastSetPointsR = VR;
        this.lastav = av;
        return new double[]{VL, VR};
    }

    public double[] calculateOutputs(double speed, double turn) {
        double[] wheelSetPoints = robotToWheels(speed, turn);
        Rsetpoint = wheelSetPoints[1];
        Lsetpoint = wheelSetPoints[0];
        double encoderLeft = left.getEncoder().getRaw() * ENCODER_TO_RADIAN;
        double encoderRight = right.getEncoder().getRaw() * ENCODER_TO_RADIAN;
        double motorOutputLeft = motorControlLeft.pidVelocity(encoderLeft, wheelSetPoints[0]);
        double motorOutputRight = motorControlRight.pidVelocity(encoderRight, wheelSetPoints[1]);
//        log("Sleft:" + wheelSetPoints[0] + " Sright:" + wheelSetPoints[1] + " Pleft:" + motorOutputLeft + " Pright:" + motorOutputRight);
        return new double[]{motorOutputLeft, motorOutputRight};
    }

    public double[] getRobotVelocities() {
//        log("Vleft: " + motorControlLeft.getDerivative() + "Vright: " + motorControlRight.getDerivative());
        return wheelsToRobot(motorControlLeft.getDerivative(), motorControlRight.getDerivative());

    }

    private double[] robotToWheels(double linear, double angular) {
        double Vleft = (linear / WHEEL_RADIUS) - (angular * WHEEL_DISTANCE) / (2 * WHEEL_RADIUS);
        double Vright = (linear / WHEEL_RADIUS) + (angular * WHEEL_DISTANCE) / (2 * WHEEL_RADIUS);
        return new double[]{Vleft, Vright};
    }

    public double[] wheelsToRobot(double Vleft, double Vright) {
        double linear = (Vright + Vleft) * WHEEL_RADIUS / 2.0;
        double angular = (Vleft - Vright) * WHEEL_RADIUS / WHEEL_DISTANCE;
        return new double[]{linear, angular};
    }

    public void updateOdometry() {
        theta = 0;


//        log("left:" + left.getEncoder().getRaw()+",right:" + right.getEncoder().getRaw());
//        double deltaWheel = (encoderLeft+encoderRight)*WHEEL_RADIUS/2.0;
//        double delta_x = ((((right.getEncoder().getRaw() + left.getEncoder().getRaw()) / 2.0) / 9036.0) * 0.659715);
//        encoders[0] = left.getEncoder().getRaw();
//        encoders[1] = right.getEncoder().getRaw();
        if (gyro == null) {
            log("gyro is null");
//            theta = thetaRobotPrev + (thetaRight - thetaRightPrev - thetaLeft + thetaLeftPrev) * (WHEEL_RADIUS / WHEEL_DISTANCE);
        } else {
            // - offsetGyro
            theta = gyro.getYaw() + 180;
            //            log("yaw: " + gyro.getName());
        }
//        log(Double.toString(theta)+","+Double.toString(gyro.getAngle()));
//        log(Double.toString(gyro.countedAngle) + "," + Double.toString(gyro.getAngle()));
//        log("Rad: "+theta);
        //x += (realVOmega[0] * Math.cos(theta) * 0.02);
//        double distance = (deltaWheel - deltaWheelPrev);
        leftMeters = encoders[0] * gearRatio * ENCODER_TO_RADIAN * WHEEL_RADIUS;
        rightMeters = encoders[1] * gearRatio * ENCODER_TO_RADIAN * WHEEL_RADIUS;
        VOmegaReal = wheelsToRobot(motorControlLeft.derivative, motorControlRight.derivative);
        distance = (leftMeters + rightMeters - rightMetersPrev - leftMetersPrev) / 2.0;
//        log("dis: " + distance);
        x += distance * Math.cos(toRadians(theta));
//        y += (realVOmega[0] * Math.sin(theta) * 0.02);
//        theta = Math.toRadians(gyro.getCountedAngle());
//
        y += distance * Math.sin(toRadians(theta));
//        log("x:" + x + ",y:" + y + ",theta:" + theta);
//        log("gyro: " + gyro.getCountedAngle());
        //log(""+realVOmega[0]);
//        log("rightEnc: "+right.getEncoder().getRaw()+" leftEnc: "+left.getEncoder().getRaw());
//        log("right_advance: "+ (encoderRight-rightEncPrev)*WHEEL_RADIUS+" left_advance: "+ (encoderLeft-leftEncPrev)*WHEEL_RADIUS);
        thetaRobotPrev = theta;
//        deltaWheelPrev = deltaWheel;
//        rightEncPrev = encoderRight;
//        leftEncPrev = encoderLeft;
//        xPrev = x;
//        yPrev = y;
        leftMetersPrev = leftMeters;
        rightMetersPrev = rightMeters;
        odometry.setX(x);
        odometry.setY(y);
        odometry.setTheta(toRadians(theta));
        odometry.setRightSetpoint(Rsetpoint);
        odometry.setLeftSetpoint(Lsetpoint);
    }

    public void preClimb() {

        if (check) {
            currentMetersLeft = left.getEncoder().getRaw() * gearRatio * ENCODER_TO_RADIAN * WHEEL_RADIUS;
            check = false;
            currentMetersRight = right.getEncoder().getRaw() * gearRatio * ENCODER_TO_RADIAN * WHEEL_RADIUS;
        }
        motorOutputLeft = motorControlLeftP.pidPosition(leftMeters, currentMetersLeft + 0.2);
        motorOutputRight = motorControlRightP.pidPosition(rightMeters, currentMetersRight + 0.2);
        if (Math.abs(motorOutputLeft) < 0.2)
            motorOutputLeft = 0;
        if (Math.abs(motorOutputRight) < 0.2)
            motorOutputRight = 0;
        direct(motorOutputLeft, motorOutputRight);
        leftMeters = left.getEncoder().getRaw() * gearRatio * ENCODER_TO_RADIAN * WHEEL_RADIUS;
        rightMeters = right.getEncoder().getRaw() * gearRatio * ENCODER_TO_RADIAN * WHEEL_RADIUS;
    }

    public void initGyro(AHRS gyro) {
        this.gyro = gyro;
        while (gyro.isCalibrating()) log("Calibrating Gyro");
        offsetGyro = gyro.getYaw();


//        log(Double.toString(this.gyro.offset));
    }

    public void direct(double leftSpeed, double rightSpeed) {
        left.set(leftSpeed);
        right.set(rightSpeed);
    }

    public double toRadians(double degrees) {
        return (degrees / 180) * Math.PI;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject returnObject = new JSONObject();
        try {
            returnObject.put("V", VOmega[0]);
            returnObject.put("omega", VOmega[1]);
            returnObject.put("realV", VOmegaReal[0]);
            returnObject.put("realOmega", VOmegaReal[1]);
            returnObject.put("leftEncoder", encoders[0]);
            returnObject.put("rightEncoder", encoders[1]);
//            returnObject.put("distance", distance);
//            returnObject.put("offset", offsetGyro);
//            returnObject.put("vlReal", motorControlLeft.derivative);
//            returnObject.put("vrReal", motorControlRight.derivative);
//            returnObject.put("Vright", -Vright);
//            returnObject.put("outputLeft", outputLeft);
//            returnObject.put("outputRight", outputRight);
//            returnObject.put("VleftEnc", encoderMeters[0]);
//            returnObject.put("VrightEnc", encoderMeters[1]);
//            returnObject.put(LEFT, left.toJSON());
//            returnObject.put("currentleft", currentMetersLeft);
//            returnObject.put("currentright", currentMetersRight);
//            returnObject.put("tom", theta);
//            returnObject.put("right Meters: ", rightMeters);
//            returnObject.put("left Meters: ", leftMeters);
//            returnObject.put("setPointOmega", setPointOmegaPrev);
//            returnObject.put(RIGHT, right.toJSON());
            returnObject.put(ODOMETRY, odometry.toJSON());
        } catch (Exception ignored) {
            log("hello");
        }
        return returnObject;
    }
}
