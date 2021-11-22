package org.firstinspires.ftc.teamcode.a_opmodes.auto.pipeline;

import android.util.Pair;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

public class TestBarcodePosition extends OpMode {
    private DuckDetector duckDetector;

    @Override
    public void init() {
        duckDetector = new DuckDetector(this, telemetry);
    }

    @Override
    public void loop() {
        duckDetector.currentlyDetected()
                .ifPresent((pipelineResultDoublePair -> {
                    telemetry.addData("Status", "Looking for a duck");
                    telemetry.addData("Detected", pipelineResultDoublePair.first);
                    telemetry.addData("Confidence", pipelineResultDoublePair.second);
                }));
    }
}
