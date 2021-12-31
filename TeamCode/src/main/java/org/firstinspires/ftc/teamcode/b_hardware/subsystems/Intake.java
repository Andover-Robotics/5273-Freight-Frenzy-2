package org.firstinspires.ftc.teamcode.b_hardware.subsystems;

import androidx.annotation.NonNull;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.SubsystemBase;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.openftc.revextensions2.ExpansionHubEx;


public class Intake extends SubsystemBase {

    public final Command cmdRunLeft = new InstantCommand(this::runLeft, this);
    public final Command cmdRunRight = new InstantCommand(this::runRight, this);
    public final Command cmdStopIntake = new InstantCommand(this::stop, this);

    private static ExpansionHubEx controlHub;
    private static ExpansionHubEx expansionHub;

    public static final double INTAKE_SPEED = 0.6;
    private static final double OUTTAKE_SPEED = 1.0;

    public MotorEx leftIntake;
    public MotorEx rightIntake;

    private double curLeftVelo, curRightVelo;
    private double curRightAmps, curLeftAmps;
    private final double INTAKE_DETECT_CONST_RPM;
    private final double INTAKE_DETECT_CONST_AMPS = 1100;
    private final double REVERSE_INTAKE_DELAY = 300;
    private static final double STOP_DELAY = 500;
    private final double IS_RUNNING_CONST_RPM = 15;
    private final double IS_RUNNING_CONST_AMPS = 30;
    // private boolean leftRunning = false, rightRunning= false;
    private double timeWhenIntake = -1; //timeWhenReverse;
    public boolean elementIntook = false;
    public boolean intookLeft = false, intookRight = false;
    public boolean reverseLeft = false, reverseRight = false;

    double currentTimeMillis;

    private enum state {
        ON,
        OFF,
        REVERSE,
        LEFT,
        RIGHT
    }

    public state runState = state.OFF;
    public OpMode opMode;

    public Intake(@NonNull OpMode opMode){

        controlHub = opMode.hardwareMap.get(ExpansionHubEx.class, "Control Hub");
        expansionHub = opMode.hardwareMap.get(ExpansionHubEx.class, "Expansion Hub 2");

        this.opMode = opMode;

        leftIntake = new MotorEx(opMode.hardwareMap, "leftIntake", Motor.GoBILDA.RPM_312);
        leftIntake.setRunMode(Motor.RunMode.RawPower);
        leftIntake.motor.setDirection(DcMotorSimple.Direction.REVERSE);

        rightIntake = new MotorEx(opMode.hardwareMap, "rightIntake", Motor.GoBILDA.RPM_312);
        rightIntake.setRunMode(Motor.RunMode.RawPower);
        rightIntake.motor.setDirection(DcMotorSimple.Direction.FORWARD);

        /*
        prevLeftVelo = 0;
        prevRightVelo = 0;
         */

        curRightVelo = 0;
        curLeftVelo = 0;

        //                    ____   <-  that is the constant ration level where intaking gets detected
        INTAKE_DETECT_CONST_RPM = 1100; //0.783333 * ((leftIntake.getMaxRPM()/60) * leftIntake.getCPR());
    }

    @Override
    public void periodic() {

        updateVeloVals();
        updateAmpVals();

        //TODO: Debug Intake Amperage Detection and Following Actions


        if (!elementIntook) {
            intookLeft = false;
            intookRight = false;
        }

        if ((wasIntakedLeft() || wasIntakedRight()) && !elementIntook) {
            timeWhenIntake = System.currentTimeMillis();
            elementIntook = true;

            if (wasIntakedLeft()) {
                intookLeft = true;
            }
            else {
                intookRight = true;
            }
        }

        else if (elementIntook && Math.abs(System.currentTimeMillis() - timeWhenIntake) > REVERSE_INTAKE_DELAY) {
            if (intookLeft) {
                reverseLeft();
            }
            else if (intookRight) {
                reverseRight();
            }
        }

        else if (elementIntook && Math.abs(System.currentTimeMillis() - timeWhenIntake) > REVERSE_INTAKE_DELAY + STOP_DELAY) {
            stop();
            elementIntook = false;
            curLeftAmps = -1;
            curRightAmps = -1;
        }

    }

    public double leftIntakeCurrentDraw() {
        return controlHub.getMotorCurrentDraw(ExpansionHubEx.CurrentDrawUnits.MILLIAMPS, 3);
    }

    public double rightIntakeCurrentDraw() {
        return controlHub.getMotorCurrentDraw(ExpansionHubEx.CurrentDrawUnits.MILLIAMPS, 2);
    }

    /*
    public void runToggle() {
        if(runState == state.OFF) {
            run();
        }
        else {
            stop();
        }
    }
    */

    public void run(){
        leftIntake.set(INTAKE_SPEED);
        rightIntake.set(INTAKE_SPEED);

        /*
        leftRunning = true;
        rightRunning = true;
         */

        runState = state.ON;
    }

    public void reverseLeft() {
        leftIntake.set(-OUTTAKE_SPEED);
    }

    public void reverseRight() {
        rightIntake.set(-OUTTAKE_SPEED);
    }

    public void runLeft(){
        if (!elementIntook)
            leftIntake.set(INTAKE_SPEED);
            //leftRunning = true;
    }

    public void runRight(){
        if (!elementIntook)
            rightIntake.set(INTAKE_SPEED);
            //rightRunning = true;
    }

    /*

    public void switchIntake(){
        if (leftRunning) {
            runRight();
        }
        else {
            runLeft();
        }
    }

     */

    public void stop(){
        leftIntake.stopMotor();
        rightIntake.stopMotor();
        /*
        leftRunning = false;
        rightRunning = false;
         */

        runState = state.OFF;
    }

    public void stopLeft() {
        leftIntake.stopMotor();

        //leftRunning = false;
    }

    public void stopRight() {
        rightIntake.stopMotor();
        //rightRunning = false;
    }

    private void updateVeloVals() {

        /*
        prevRightVelo = curRightVelo;
        prevLeftVelo  = curLeftVelo;
         */

        curLeftVelo = leftIntake.getVelocity();
        curRightVelo = rightIntake.getVelocity();
    }

    private void updateAmpVals() {

        /*
        prevRightVelo = curRightVelo;
        prevLeftVelo  = curLeftVelo;
         */

        //keep track of maximum amperage
        curLeftAmps = Math.max(curLeftAmps, leftIntakeCurrentDraw());
        curRightAmps = Math.max(curRightAmps, rightIntakeCurrentDraw());
    }

    public boolean wasIntakedLeft() {
        return curLeftAmps > INTAKE_DETECT_CONST_AMPS;
                //(curLeftVelo < INTAKE_DETECT_CONST_RPM && isLeftIntaking())
    }

    public boolean wasIntakedRight() {
        return curRightAmps > INTAKE_DETECT_CONST_AMPS;
                //(curRightVelo < INTAKE_DETECT_CONST_RPM && isRightIntaking()) || rightIntakeCurrentDraw() > INTAKE_DETECT_CONST_AMPS;
    }


    public double getCurLeftVelo() { return curLeftVelo; }

    public double getCurRightVelo() { return curRightVelo; }

    /*
    public double getPrevLeftVelo() { return prevLeftVelo; }
    public double getPrevRightVelo() { return prevRightVelo; }
     */

    public boolean isLeftIntaking() {
        return leftIntake.getVelocity() > IS_RUNNING_CONST_RPM || leftIntakeCurrentDraw() > IS_RUNNING_CONST_AMPS;
    }

    public boolean isRightIntaking() {
        return rightIntake.getVelocity() > IS_RUNNING_CONST_RPM || rightIntakeCurrentDraw() > IS_RUNNING_CONST_AMPS;
    }

}
