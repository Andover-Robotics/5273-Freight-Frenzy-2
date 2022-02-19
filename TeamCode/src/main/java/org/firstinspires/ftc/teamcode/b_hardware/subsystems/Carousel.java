package org.firstinspires.ftc.teamcode.b_hardware.subsystems;
//todo implement with motor instead of CR servo
import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.CommandBase;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.SubsystemBase;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoController;

import org.firstinspires.ftc.teamcode.GlobalConfig;

@Config
public class Carousel extends SubsystemBase {
    public final Command cmdRunRed = new InstantCommand(this::runRed, this);
    public final Command cmdRunBlue = new InstantCommand(this::runBlue, this);
    public final Command cmdStopCarousel = new InstantCommand(this::stop, this);

    public static double OPTIMAL_RPM = 60;
    public static final double MOTOR_SPEED = 1150;
    public static final double TICKS_PER_REVOLUTION = 145.1;
    public static final double VELOCITY = OPTIMAL_RPM * TICKS_PER_REVOLUTION / 60.0; //531 rpm

    private final MotorEx motor;
    private enum State {
        RED_ON,
        BLUE_ON,
        OFF
    }
    private State runState = State.OFF;


    public Carousel(@NonNull OpMode opMode){
        motor = new MotorEx(opMode.hardwareMap, "carousel", Motor.GoBILDA.RPM_1150);
        motor.setRunMode(Motor.RunMode.VelocityControl);
        motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        motor.setInverted(GlobalConfig.alliance == GlobalConfig.Alliance.RED);
    }

    public void runRed() {
        motor.set(-.2);
        runState = State.RED_ON;
    }

    public void runBlue() {
       motor.set(.2);
        // motor.setVelocity(VELOCITY);
        runState = State.BLUE_ON;
    }

    public void runAtRPM(boolean red, int rpm) {
        motor.setVelocity(((red) ? -1 : 1) * rpm / 60 * TICKS_PER_REVOLUTION);
        if (red) {
            runState = State.BLUE_ON;
        }
        else {
            runState = State.RED_ON;
        }
    }

    public void toggleBlue() {
        if(runState == State.BLUE_ON) {
            stop();
        }
        else {
            runBlue();
        }
    }

    public void toggleRed() {
        if(runState == State.RED_ON) {
            stop();
        }
        else  {
            runRed();
        }
    }

    public void stop() {
        //motor.stopMotor();
        motor.set(0.0);
        runState = State.OFF;
    }

    public MotorEx getMotor() {
        return motor;
    }


}
