package org.firstinspires.ftc.teamcode.a_opmodes.auto


import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.acmerobotics.roadrunner.trajectory.Trajectory
import com.arcrobotics.ftclib.command.*
import org.firstinspires.ftc.teamcode.GlobalConfig
import org.firstinspires.ftc.teamcode.b_hardware.Bot
import org.firstinspires.ftc.teamcode.b_hardware.subsystems.Outtake
import java.lang.Math.toRadians
import kotlin.math.atan

object AutoCommands {

    // remeber to use endPose and pos to make it easier please

    // wrap trajectories in a command
    // wrap outtake controls in commands (BucketUp, BucketDown)
    // wrap carousel controls in commands
    // wrap intake controls in commands
    // waiting should be a command


    val Double.toRadians get() = (toRadians(this))

    val offset = -90.0.toRadians

    class FollowTrajectory(val bot: Bot, val trajectory: Trajectory) : CommandBase() {
        override fun initialize() = bot.roadRunner.followTrajectoryAsync(trajectory)
        override fun execute() = bot.roadRunner.update()
        override fun isFinished() = !bot.roadRunner.isBusy
    }

    fun generateRunCarouselFeature(drift: Vector2d = Vector2d()): CommandBase {
        val bot = Bot.getInstance()
        val pos = bot.roadRunner.poseEstimate
        val startTangent = if (pos.x > 56) 315.0.toRadians + offset else pos.heading
        val traj = bot.roadRunner.trajectoryBuilder(pos, startTangent)
                .splineToLinearHeading(
                        if(GlobalConfig.alliance == GlobalConfig.Alliance.RED)
                            Pose2d(55.0 + drift.x, -58.0 + drift.y, 0.0.toRadians + offset)
                        else
                            Pose2d(-55.0 + -drift.x, -58.0 + drift.y, 0.0.toRadians + offset),
                        if(GlobalConfig.alliance == GlobalConfig.Alliance.RED) 45.0.toRadians + offset
                        else -45.0.toRadians + offset)
        return SequentialCommandGroup(

                ParallelCommandGroup(
                        FollowTrajectory(bot, traj.build()),
                        InstantCommand(if (GlobalConfig.alliance == GlobalConfig.Alliance.RED) bot.carousel::runRed else bot.carousel::runBlue),
                        bot.outtake.RunSlides(Outtake.RETRACTED, Outtake.SlideState.RETRACTED)),
                WaitCommand(1800),
                InstantCommand(bot.carousel::stop))
    }

    fun generateExitWarehouseFeature(): CommandBase {
        val bot = Bot.getInstance()
        return ParallelCommandGroup(
            bot.outtake.RunSlides(Outtake.RETRACTED, Outtake.SlideState.RETRACTED),
            FollowTrajectory(bot, bot.roadRunner.trajectoryBuilder(bot.roadRunner.poseEstimate, 180.0.toRadians)
                    .strafeTo(Vector2d(64.0, 0.0)).build())
        )
    }

    fun generateOuttakeFeature(drift: Vector2d = Vector2d()): CommandBase {
        val bot = Bot.getInstance()
        val pos = bot.roadRunner.poseEstimate
        val endPos = Pose2d(50.0-4, -12.0+4, 0.0.toRadians + offset) + Pose2d(drift, 0.0)
        var startTangent = 30.0.toRadians + offset

        if (!bot.isInWarehouse)
        {
            startTangent = atan((pos.y - endPos.y)/(pos.x-endPos.x)).toRadians + offset
        }

        val traj = bot.roadRunner.trajectoryBuilder(pos, startTangent)
                .splineToLinearHeading(endPos, 270.0.toRadians + offset)

        return SequentialCommandGroup(
                ParallelCommandGroup(
                        FollowTrajectory(bot, traj.build()),
                        SequentialCommandGroup(
                                WaitCommand(600),
                                InstantCommand(bot.intake::stop, bot.intake),
                                InstantCommand(bot.outtake::closeLeftFlap, bot.outtake),
                                InstantCommand(bot.outtake::closeRightFlap, bot.outtake),
                                bot.outtake.RunSlides(Outtake.TOP_GOAL_POS,
                                        Outtake.SlideState.AT_TOP_GOAL))),
                InstantCommand(bot.outtake::flipBucket, bot.outtake),
                WaitCommand(1000),
                InstantCommand(bot.outtake::unFlipBucket, bot.outtake)
        )
    }
    fun generateIntakeFeature(drift: Vector2d = Vector2d()): CommandBase {
        val bot = Bot.getInstance()
        val pos = bot.roadRunner.poseEstimate
        val endPos = Pose2d(64.0, 58.0, 0.0 + offset)
        val startTangent = 90.0.toRadians + offset

        val trajToBarrier = bot.roadRunner.trajectoryBuilder(pos, startTangent)
//            .lineToLinearHeading(Pose2d(64.0, 20.0, 0.0 + offset))
            .splineToLinearHeading(endPos, 210.0.toRadians+ offset)
            .build()
//        val trajToWarehouse = bot.roadRunner.trajectoryBuilder(trajToBarrier.end(), 180.0.toRadians)
//            .strafeTo(Vector2d(64.0, 58.0))
//            .build()

//if (GlobalConfig.alliance == GlobalConfig.Alliance.RED)
        return SequentialCommandGroup(
                InstantCommand(bot.outtake::closeLeftFlap, bot.outtake),
                InstantCommand(bot.outtake::openRightFlap, bot.outtake),
                InstantCommand(bot.intake::runRight, bot.intake),
                ParallelDeadlineGroup(FollowTrajectory(bot, trajToBarrier),
                        SequentialCommandGroup(
                            bot.outtake.RunSlides(Outtake.RETRACTED, Outtake.SlideState.RETRACTED),
                            InstantCommand(bot.intake::runRight, bot.intake),
                            InstantCommand(bot.intake::reverseLeft, bot.intake))),
                InstantCommand(bot.intake::reverseRight, bot.intake),
                WaitCommand(500))
    }

    fun generateParkDepotFeature(drift: Vector2d = Vector2d()): CommandBase {
        val bot = Bot.getInstance()
        val traj = bot.roadRunner.trajectoryBuilder(bot.roadRunner.poseEstimate)
            .strafeTo(Vector2d(39.0, -63.0) + drift).build()
        return ParallelDeadlineGroup(FollowTrajectory(bot, traj),
            bot.outtake.RunSlides(Outtake.RETRACTED, Outtake.SlideState.RETRACTED))
    }

    fun generatePreloadedFreightFeature(drift: Vector2d = Vector2d()): CommandBase {
        val bot = Bot.getInstance()
        val pos = bot.roadRunner.poseEstimate
        val startTangent = if (pos.x > 56) 315.0.toRadians + offset else pos.heading
        val traj = bot.roadRunner.trajectoryBuilder(pos, startTangent)
                .lineToSplineHeading(Pose2d(Vector2d(.0, -24.0) + drift, (-45.0).toRadians + offset))

        return SequentialCommandGroup(
                bot.outtake.cmdCloseLeftFlap,
                bot.outtake.cmdCloseRightFlap,
                ParallelCommandGroup(
                        FollowTrajectory(bot, traj.build()),
                        SequentialCommandGroup(
                                WaitCommand(600),
                                bot.outtake.RunSlides(Outtake.TOP_GOAL_POS,
                                        Outtake.SlideState.AT_TOP_GOAL))),
                bot.outtake.cmdFlipBucket,
                WaitCommand(1000),
                bot.outtake.cmdUnflipBucket)
    }

    private val Bot.isInWarehouse get() =
        roadRunner.poseEstimate.y > 24 && roadRunner.poseEstimate.x > 24
}