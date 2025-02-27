package de.saring.exerciseviewer.parser.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.saring.exerciseviewer.core.EVException;
import de.saring.exerciseviewer.data.EVExercise;
import de.saring.exerciseviewer.data.ExerciseAltitude;
import de.saring.exerciseviewer.data.ExerciseSample;
import de.saring.exerciseviewer.data.ExerciseSpeed;
import de.saring.exerciseviewer.data.HeartRateLimit;
import de.saring.exerciseviewer.data.Lap;
import de.saring.exerciseviewer.data.LapAltitude;
import de.saring.exerciseviewer.data.LapSpeed;
import de.saring.exerciseviewer.data.LapTemperature;
import de.saring.exerciseviewer.data.Position;
import de.saring.exerciseviewer.data.RecordingMode;
import de.saring.exerciseviewer.parser.AbstractExerciseParser;
import de.saring.exerciseviewer.parser.ExerciseParserInfo;
import de.saring.util.unitcalc.CalculationUtils;

/**
 * This implementation of an ExerciseParser is for reading PWX files of the
 * Timex Race Trainer watch.  It will likely work with other PWX files
 * downloaded from the Training Peaks website.  It works well and has been
 * tested with Chrono PWX files but has not been tested with Interval PWX files.
 * <br/>
 * It is assumed that the exercise files have the extension ".pwx".
 * <br/>
 * This file has been completely rewritten from the initial version
 * that was based on PolarHsrRawParser.java by Remco den Breeje
 * which is based on PolarSRawParser.java by Stefan Saring
 * <br/>
 * TODO: This parser contains a lot of unused code (commented out),
 * remove it when not needed anymore.
 * <p/>
 * 9/10/2010 Version 1.2
 * Added support for Global Trainer Pwx Files
 * Changed Lap Distance to Distance since beginning of exercise
 * 01/03/2012 Version 1.3
 * Added support for Timex Ironman Run Trainer Pwx Files
 *
 * @author Robert C. Schultz, Stefan Saring
 * @version 1.3
 */
public class TimexPwxParser extends AbstractExerciseParser {

    /**
     * Informations about this parser.
     */
    private final ExerciseParserInfo info = new ExerciseParserInfo("Timex PWX", List.of("pwx", "PWX"));

    private static class MinMaxAvg {
        private float min = 0;
        private float max = 0;
        private float avg = 0;

        public void setMin(float in) {
            min = in;
        }

        public float getMin() {
            return min;
        }

        public void setMax(float in) {
            max = in;
        }

        public float getMax() {
            return max;
        }

        public void setAvg(float in) {
            avg = in;
        }

        public float getAvg() {
            return avg;
        }
    }

    private MinMaxAvg node2MinMaxAvg(Node inNode) {
        MinMaxAvg result = new MinMaxAvg();
        NamedNodeMap attributes = inNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            if (attributes.item(i).getNodeName().equals("max")) {
                result.setMax(Float.valueOf(attributes.item(i).getTextContent()));
            } else if (attributes.item(i).getNodeName().equals("min")) {
                result.setMin(Float.valueOf(attributes.item(i).getTextContent()));
            } else if (attributes.item(i).getNodeName().equals("avg")) {
                result.setAvg(Float.valueOf(attributes.item(i).getTextContent()));
            }
        }
        return result;
    }

    private static class SummaryData {
        private double beginning = 0;
        private double duration = 0;
        private int work = 0;
        private MinMaxAvg hr;
        //        private double durationStopped = 0;
//        private float tss = 0;
//        private int normalizedPower = 0;
        private MinMaxAvg speed;
        //        private MinMaxAvg power;
//        private MinMaxAvg torque;
//        private MinMaxAvg cadence;
        private float distance = 0;
        private MinMaxAvg altitude;
//        private MinMaxAvg temperature;
//        private int variabilityIndex = 0;
//        private float climbingElevation = 0;

        public void setBeginning(double in) {
            beginning = in;
        }

        public double getBeginning() {
            return beginning;
        }

        public void setDuration(double in) {
            duration = in;
        }

        public double getDuration() {
            return duration;
        }

        public void setWork(int in) {
            work = in;
        }

        public int getWork() {
            return work;
        }

        public void setHr(MinMaxAvg in) {
            hr = in;
        }

        public MinMaxAvg getHr() {
            return hr;
        }

        //        public void setDurationStopped(double in){ durationStopped = in; }
//        public double getDurationStopped(){ return durationStopped; }
//        public void setTss(float in){ tss = in; }
//        public float getTss(){ return tss; }
//        public void setNormalizedPower(int in){ normalizedPower = in; }
//        public int getNormalizedPower(){ return normalizedPower; }
        public void setSpeed(MinMaxAvg in) {
            speed = in;
        }

        public MinMaxAvg getSpeed() {
            return speed;
        }

        //        public void setPower(MinMaxAvg in){ power = in; }
//        public MinMaxAvg getPower(){ return power ; }
//        public void setTorque(MinMaxAvg in){ torque = in; }
//        public MinMaxAvg getTorque(){ return torque; }
//        public void setCadence(MinMaxAvg in){ cadence = in; }
//        public MinMaxAvg getCadence(){ return cadence; }
        public void setDistance(float in) {
            distance = in;
        }

        public float getDistance() {
            return distance;
        }
	  public void printDistance(){
		 System.out.println(distance);
	  }

        public void setAltitude(MinMaxAvg in) {
            altitude = in;
        }

        public MinMaxAvg getAltitude() {
            return altitude;
        }
//        public void setTemperature(MinMaxAvg in){ temperature  = in; }
//        public MinMaxAvg getTemperature(){ return temperature; }
//        public void setVariabilityIndex(int in){ variabilityIndex = in; }
//        public int getVariabilityIndex(){ return variabilityIndex; }
//        public void setClimbingElevation(float in){ climbingElevation = in; }
//        public float getClimbingElevation(){ return climbingElevation; }
    }

    @Override
    public ExerciseParserInfo getInfo() {
        return info;
    }

    int countNodeItems(Node node, String string2count) {
        // Given a Node and a Child Node Name, count the number of children with that node name
        NodeList children = node.getChildNodes();

        int numChildren = children.getLength();
        String currentNodeName = null;
        int numMatches = 0;


        for (int i = 0; i < numChildren; i++) {
            currentNodeName = children.item(i).getNodeName();
            if (currentNodeName.equals(string2count)) {
                numMatches++;
            }
        }
        return numMatches;
    }

    private EVExercise parseWorkoutNode(EVExercise exercise, Node workoutNode) {
        NodeList children = workoutNode.getChildNodes();
        String childName;
        for (int i = 0; i < children.getLength(); i++) {
            childName = children.item(i).getNodeName();
            switch (childName) {
                case "athlete":
                    // Nothing to do with this yet...or is there?
                    break;
                case "goal":
                    // Not in files downloaded directly from the Timex 843/844
                    // Probably is in the files downloaded from the online software
                    break;
                case "sportType":
                    // obtain sportType
                    exercise.setSportType(children.item(i).getTextContent());
                    break;
                case "cmt":
                case "code":
                    // Not implemented
                    break;
                case "device":
                    // parse device
                    exercise = parseWorkoutDeviceNode(exercise, children.item(i));
                    // The passing an object and then assigning the result of the method to the same object is akward to me.
                    // It seems like that could result in a lot of time moving data.  My understanding is that is not the case
                    // in Java though.
                case "time":
                    // obtain start time
                    try {
                        String strDateTime = children.item(i).getTextContent();
                        exercise.setDateTime(LocalDateTime.parse(strDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } catch (Exception e) {
                        exercise.setDateTime(null);
                    }
                    break;
                case "summarydata":
                    // parse workout summary data
                    SummaryData workoutSummary = parseSummaryData(children.item(i));
                    exercise.setDuration((int) workoutSummary.getDuration() * 10);
                    exercise.setSumExerciseTime((int) workoutSummary.getDuration() / 60); // Not sure why these are different.
                    exercise.setSumRideTime((int) workoutSummary.getDuration() / 60);  // Assume some watches keep track of bike specific time..This one doesn't
                    exercise.setEnergy((int) (workoutSummary.getWork() * (0.238845896627495939619))); // Convert to Calories first
                    //exercise.setEnergyTotal((int) (workoutSummary.getWork() * (0.238845896627495939619))); // Using the value in device/extensions
                    if (workoutSummary.getHr() != null) {
                        exercise.setHeartRateMax((short) workoutSummary.getHr().getMax());
                        // exercise.setHeartRateMin((short) workoutSummary.getHr().getMin()); // Not implemented in EVExercise
                        exercise.setHeartRateAVG((short) workoutSummary.getHr().getAvg());
                    }
                    exercise.setOdometer((int) workoutSummary.getDistance() / 1000);
                    if (workoutSummary.getSpeed() != null) {
                        int distance = (int) workoutSummary.getDistance();
                        float speedAvg = workoutSummary.getSpeed().getAvg() * (float) 3.6;
                        float speedMax = workoutSummary.getSpeed().getMax() * (float) 3.6;
                        exercise.setSpeed(new ExerciseSpeed(speedAvg, speedMax, distance));
                    }
                    if (workoutSummary.getAltitude() != null) {
                        short altitudeMin = (short) workoutSummary.getAltitude().getMin();
                        short altitudeAvg = (short) workoutSummary.getAltitude().getAvg();
                        short altitudeMax = (short) workoutSummary.getAltitude().getMax();
                        exercise.setAltitude(new ExerciseAltitude(altitudeMin, altitudeAvg, altitudeMax, 0, 0));
                    }
                    break;
                case "segment":
                case "sample":
                    // This is handled after parsing everything else
                    break;
                case "extension":
                    // Used for Timex Global Trainer and possibly others.
                    exercise = parseWorkoutExtensionNode(exercise, children.item(i));
                    break;
            }
        }
        // parse lap segments
        exercise = parseWorkoutSegments(exercise, workoutNode);
        // parse samples
        exercise = parseWorkoutSamples(exercise, workoutNode);
        return exercise;
    }

    private EVExercise parseWorkoutExtensionNode(EVExercise exercise, Node workoutExtensionNode) {
        // Used for Global Trainer
        NodeList children = workoutExtensionNode.getChildNodes();
        String childName;
        for (int i = 0; i < children.getLength(); i++) {
            childName = children.item(i).getNodeName();//
            if (childName.equals("ascent")) {
                exercise.getAltitude().setAscent(Integer.valueOf(children.item(i).getTextContent()));
            } else if (childName.equals("descent")) {
                // obtain descent - not used in EVExercise
            } else if (childName.equals("points")) {
                // points - not used in EVExercise
            }
        }
        return exercise;
    }

    private EVExercise parseWorkoutDeviceNode(EVExercise exercise, Node deviceNode) {
        NodeList children = deviceNode.getChildNodes();
        String childName;
        String make = "";
        String model = "";

        for (int i = 0; i < children.getLength(); i++) {
            childName = children.item(i).getNodeName();
            if (childName.equals("extension")) {
                // parse extension
                exercise = parseDeviceExtensionNode(exercise, children.item(i));
            } else if (childName.equals("make")) {
                make = children.item(i).getTextContent();
            } else if (childName.equals("model")) {
                model = children.item(i).getTextContent();
                if (model.equals("Global Trainer") || model.equals("Run Trainer")) {
                    exercise = setGlobalTrainerRecordingMode(exercise);
                    exercise = setGlobalTrainerZones(exercise);
                }
            } else if (childName.equals("stopdetectionsetting")) {
                // obtain stopdetectionsetting        
            } else if (childName.equals("elevationchangesetting")) {
                // obtain elevationchangesetting        
            }
        }

        exercise.setDeviceName((make.isEmpty() ? "" : make + " ") + model);
        return exercise;
    }

    private EVExercise setGlobalTrainerRecordingMode(EVExercise exercise) {
        RecordingMode recMode = new RecordingMode();

        recMode.setHeartRate(true);
        recMode.setLocation(true);
        recMode.setCadence(false);
        recMode.setAltitude(true);
        recMode.setSpeed(true);
        recMode.setBikeNumber(null);
        recMode.setIntervalExercise(false); //

        exercise.setRecordingMode(recMode);
        return exercise;
    }

    private EVExercise setGlobalTrainerZones(EVExercise exercise) {
        for (int i = 0; i < 6; i++) {
            short upperHeartRate = (short) (50 + (i + 1) * 25);
            short lowerHeartRate = (short) (50 + i * 25);
            HeartRateLimit hrLimit = new HeartRateLimit(lowerHeartRate, upperHeartRate, null, 0, null, true);
            exercise.getHeartRateLimits().add(hrLimit);
        }
        return exercise;
    }

    private EVExercise parseDeviceExtensionNode(EVExercise exercise, Node deviceExtensionNode) {
        NodeList children = deviceExtensionNode.getChildNodes();
        String childName;
        for (int i = 0; i < children.getLength(); i++) {
            childName = children.item(i).getNodeName();//
            if (childName.equals("settings")) {
                exercise = parseDeviceExtensionSettingsNode(exercise, children.item(i));
            } else if (childName.equals("stoppage")) {
                // obtain stoppage - not used in EVExercise
            }
        }
        return exercise;
    }

    private EVExercise parseDeviceExtensionSettingsNode(EVExercise exercise, Node deviceExtensionSettingsNode) {
        // None of this data is explicitly specified in the pwx.xsd.
        // It is in the pwx files from the Timex watch though.
        //------------------------------------------------------------
        NodeList children = deviceExtensionSettingsNode.getChildNodes();
        String childName;
        // Create and Initialize Heart Rate Limits
        HeartRateLimit Zones[] = new HeartRateLimit[6];
        for (int i = 0; i < 6; i++) {
            Zones[i] = new HeartRateLimit((short) 0, (short) 0, null, 0, null, true);
        }

        for (int i = 0; i < children.getLength(); i++) {
            childName = children.item(i).getNodeName();// 
            if (childName.equals("CHRMaxHR") || childName.equals("HRMMaxHR")) {
                // Obtain Max HR - This is basis for Timex Zones
                short HRMMaxHR = Short.valueOf(children.item(i).getTextContent());
                double HRZonesPercentages[] = {1, .9, .8, .7, .6, .5};
                for (int k = 0; k < 5; k++) {
                    short upperHeartRate = (short) (HRZonesPercentages[k] * HRMMaxHR);
                    short lowerHeartRate = (short) (1 + HRZonesPercentages[k + 1] * HRMMaxHR);
                    Zones[k] = new HeartRateLimit(lowerHeartRate, upperHeartRate, null, 0, null, true);
                }
            } else if (childName.equals("CHRManualZoneHigherLimit") || childName.equals("HRMBpmManHi")) {
                // obtain Manual Zone Higher Limit
                Zones[5].setUpperHeartRate(Short.valueOf(children.item(i).getTextContent()));
            } else if (childName.equals("CHRManualZoneLowerLimit") || childName.equals("HRMBpmManLo")) {
                // obtain Manual Zone Lower Limit          
                Zones[5].setLowerHeartRate(Short.valueOf(children.item(i).getTextContent()));
            } else if (childName.equals("HasHRMData")) {
                // does file have hrm data   
            } else if (childName.equals("KCalPerDevice")) {
                // obtain kCalPerDevice        
                exercise.setEnergyTotal(Integer.valueOf(children.item(i).getTextContent()));
            } else if (childName.equals("WorkoutType")) {
                // obtain workout type - If not Chrono, then stop parsing since intervals aren't yet implemented
                if (!children.item(i).getTextContent().equals("Chrono")) {
                    // Not sure how to handle this... I want it to stop parsing and report an unsupported file.

                }
            } else {
                // obtain Alarm flags 1-3 (Enabled/Disabled)
                // obtain Alarm Hours 1-3
                // obtain Alarm Minutes 1-3
                // obtain Alarm Type 1-3
                // obtain Application that generated the file
                // obtain AVG Lap time
                // obtain Best Lap time
                // obtain best lap number
                // obtain average HRs for each lap (CHRDatabaseTable##)
                // Don't Care about HRM display format
                // obtain Time In Target Zone
                // obtain recovery end bpm
                // obtain recovery start bpm
                // obtain CHRSplitDuration
                // obtain CHRStatus
                // obtain Target HR Zone
                // obtain ManZone Percentage Hi (This is different then the Manual HR Zone in bpm)
                // obtain ManZone Percentage Low (This is different then the Manual HR Zone in bpm)
                // obtain weight
                // obtain weight units
                // does file have Recovery BPM
                // obtain interval data --- Not implementing this yet ---
                // obtain watch ID - This is a String
                // obtain watch manufacturer
                // obtain watch model
                // obtain version number
                // obtain workout number - Not sure what this number is
            }
        }
        // don't care about Button Beep, Hourly Chime, Night Mode, Night Mode Duration, Display Format
        // don't care about HRMAlertApp
        // don't care about Display Units (need to check to see if changing to Percentage changes the way data is stored.)
        // don't care about out of zone alert
        // don't care about RCVYPresetIndex
        // don't care about the Timer data
        // don't care about the Time of Day format /Time Zone (Might care about the time zone if it was actual time zone but its not)
        // don't care if - is file Locked
        // do laps overflow - might care about this but not sure when
        // don't care about some ucaddr# values
        RecordingMode recMode = new RecordingMode();

        recMode.setHeartRate(true);
        recMode.setCadence(false);
        recMode.setAltitude(false);
        recMode.setSpeed(false);
        recMode.setBikeNumber(null);
        recMode.setIntervalExercise(false); //

        exercise.setRecordingMode(recMode);
        exercise.getHeartRateLimits().addAll(List.of(Zones));
        return exercise;
    }

    private SummaryData parseSummaryData(Node summaryDataNode) {
        SummaryData nodeSummaryData = new SummaryData();
        NodeList children = summaryDataNode.getChildNodes();

        String childName;
        for (int i = 0; i < children.getLength(); i++) {
            childName = children.item(i).getNodeName();
            if (childName.equals("beginning")) {
                // obtain beginning time
                nodeSummaryData.setBeginning(Double.valueOf(children.item(i).getTextContent()));
            } else if (childName.equals("duration")) {
                // obtain duration
                nodeSummaryData.setDuration(Double.valueOf(children.item(i).getTextContent()));
            } else if (childName.equals("hr")) {
                // obtain hr (MinMaxAvg)  (bpm)
                nodeSummaryData.setHr(node2MinMaxAvg(children.item(i)));
            } else if (childName.equals("work")) {
                // obtain work (Apparently Not used in Laps) (kJ)
                nodeSummaryData.setWork(Integer.valueOf(children.item(i).getTextContent()));
            } else if (childName.equals("spd")) {
                // obtain spd (MinMaxAvg) (meters/second)
                nodeSummaryData.setSpeed(node2MinMaxAvg(children.item(i)));
            } else if (childName.equals("alt")) {
                // obtain altitude (MinMaxAvg) (meters)
                nodeSummaryData.setAltitude(node2MinMaxAvg(children.item(i)));
            } else if (childName.equals("dist")) {
                // obtain distance (meters)
                nodeSummaryData.setDistance(Float.valueOf(children.item(i).getTextContent()));
            }
            // 1st time its for the entire workout
            // remaining times is for the Laps
            // obtain duration stopped
            // obtain tss
            // obtain normalizedPower (watts)
            // obtain pwr (MinMaxAvg) (watts)
            // obtain torq (MinMaxAvg) (nM)
            // obtain cadence (MinMaxAvg) (rpm)
            // obtain temp (MinMaxAvg) (C)
            // obtain variabilityIndex - Not sure what this is
            // obtain climbingelevation
        }
        return nodeSummaryData; // Probably don't want to pass and return the Exercise itself.
    }

    private EVExercise parseWorkoutSegments(EVExercise exercise, Node workoutNode) {
        ArrayList<Lap> laps = new ArrayList<>();

        // obtain segment name  ( Either laps or Workout Summary )
        // parse segment summary data
        // Create and initialize a holding Lap

        // Finished Holding Lap
        NodeList children = workoutNode.getChildNodes();
        NodeList segmentChildren = null;
        String childName;
        float runningDistance = 0;
        for (int i = 0; i < children.getLength(); i++) {
            childName = children.item(i).getNodeName();
            if (childName.equals("segment")) {
                segmentChildren = children.item(i).getChildNodes();
                Lap lap = new Lap();
                LapSpeed lapSpd = new LapSpeed(0f, 0f,
                        402, // I typically mark each lap at the 1/4 mile.  A popup might be nice to fill in the rest.
                        null);
                lap.setSpeed(lapSpd);
                lap.setTemperature(new LapTemperature((short) 25));
                for (int j = 0; j < segmentChildren.getLength(); j++) {
                    childName = segmentChildren.item(j).getNodeName();
                    if (childName.equals("summarydata")) {
                        SummaryData segmentSummary = parseSummaryData(segmentChildren.item(j));
                        lap.setTimeSplit((int) ((segmentSummary.getDuration() + segmentSummary.getBeginning()) * 10));
                        if (segmentSummary.getDistance() != 0) {
                            runningDistance += segmentSummary.getDistance();
                            lapSpd.setDistance((int) runningDistance);
                            lapSpd.setSpeedAVG((float) (3.600 * segmentSummary.getDistance() / segmentSummary.getDuration())); // Assumes 1/4 Mile Lap
                            lapSpd.setSpeedEnd((float) 0.0);
                        } else {
                            runningDistance += 402.336;
                            lapSpd.setDistance((int) runningDistance);
                            lapSpd.setSpeedAVG((float) (3.6 * 402.336 / segmentSummary.getDuration())); // Assumes 1/4 Mile Lap
                            lapSpd.setSpeedEnd((float) 0.0);
                        }
                        lap.setSpeed(lapSpd);
                        if (segmentSummary.getHr() != null) {
                            if (segmentSummary.getHr().getAvg() > 0) {
                                lap.setHeartRateAVG((short) segmentSummary.getHr().getAvg());
                            }
                            if (segmentSummary.getHr().getMax() > 0) {
                                lap.setHeartRateMax((short) segmentSummary.getHr().getMax());
                            }
                        }
                        if (segmentSummary.getAltitude() != null) {
                            short lapAltitude = (short) segmentSummary.getAltitude().getMax();
                            int lapAscent = (int) (segmentSummary.getAltitude().getMax() - segmentSummary.getAltitude().getMin());
                            lap.setAltitude(new LapAltitude(lapAltitude, lapAscent, 0));
                        }
                    }
                }

                // sometimes there are laps (mostly the last one) with lap length 0 => ignore them
                Lap previousLap = laps.isEmpty() ? null : laps.get(laps.size() - 1);
                if (previousLap == null || lap.getTimeSplit() > previousLap.getTimeSplit()) {
                    laps.add(lap);
                }
            }
        }

        if (!laps.isEmpty()) {
            exercise.getLapList().addAll(laps);
        }
        return exercise;
    }

    private static float getDistanceFromPositions(Position startPosition, Position stopPosition) { //float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6369.6; //3958.75;
        double dLat = Math.toRadians(stopPosition.getLatitude() - startPosition.getLatitude());
        double dLng = Math.toRadians(stopPosition.getLongitude() - startPosition.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(startPosition.getLatitude())) * Math.cos(Math.toRadians(stopPosition.getLatitude()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;

        int meterConversion = 1000; // 1609;

        if (dist < 0) {
            dist = 0 - dist;
        }
        return (float) (dist * meterConversion);
    }

    private EVExercise parseWorkoutSamples(EVExercise exercise, Node workoutNode) {
        // obtain all the sample data.
        int totalSamples = countNodeItems(workoutNode, "sample");
        int currentSampleNumber = 0;
        float lastDistance = 0;
        boolean distanceinsample = false;
        boolean firstsample = true;
        double lastOffset = 0;
        double currentOffset = 0;
        Position lastPosition = new Position(0, 0);
        NodeList children = workoutNode.getChildNodes();
        NodeList sampleChildren = null;
        String childName;
        ExerciseSample lastSample = new ExerciseSample(); // Stop the jitters... assumes no
        Double latitude = 0.0, longitude = 0.0;
        double belowZone[] = {0, 0, 0, 0, 0, 0};
        double inZone[] = {0, 0, 0, 0, 0, 0};
        double aboveZone[] = {0, 0, 0, 0, 0, 0};
        int istop = children.getLength(); // getLength() is a slow function so keep it out of the loop.
        for (int i = 0; i < istop; i++) {
            childName = children.item(i).getNodeName();
            if (childName.equals("sample")) {
                ExerciseSample sample = new ExerciseSample();
                sample.setHeartRate((short) 0);
                sampleChildren = children.item(i).getChildNodes();
                int jstop = sampleChildren.getLength();
                for (int j = 0; j < jstop; j++) {
                    childName = sampleChildren.item(j).getNodeName();
                    if (childName.equals("timeoffset")) {
                        if (currentOffset != 0)
                            lastOffset = currentOffset;
                        currentOffset = Double.valueOf(sampleChildren.item(j).getTextContent());
                        sample.setTimestamp((long) (1000 * currentOffset));
                    } else if (childName.equals("hr")) {
                        sample.setHeartRate(Short.valueOf(sampleChildren.item(j).getTextContent()));
                    } else if (childName.equals("spd")) {
                        sample.setSpeed((float) 3.6 * Float.valueOf(sampleChildren.item(j).getTextContent()).floatValue());
                    } else if (childName.equals("pwr")) {
                        // Not implemented in ExerciseSample class
                    } else if (childName.equals("torq")) {
                        // Not implemented in ExerciseSample class
                    } else if (childName.equals("cad")) {
                        sample.setCadence(Short.valueOf(sampleChildren.item(j).getTextContent()));
                        exercise.getRecordingMode().setCadence(true);
                    } else if (childName.equals("dist")) {
                        double dist = Double.valueOf(sampleChildren.item(j).getTextContent());
                        sample.setDistance((int) Math.round(dist));
                        distanceinsample = true;
                    } else if (childName.equals("lat")) {
                        latitude = Double.valueOf(sampleChildren.item(j).getTextContent());
                    } else if (childName.equals("lon")) {
                        longitude = Double.valueOf(sampleChildren.item(j).getTextContent());
                    } else if (childName.equals("alt")) {
                        sample.setAltitude(Float.valueOf(sampleChildren.item(j).getTextContent()).shortValue());
                    } else if (childName.equals("temp")) {
                        sample.setTemperature(Float.valueOf(sampleChildren.item(j).getTextContent()).shortValue());
                    } else if (childName.equals("time")) {
                        // Not implemented in ExerciseSample
                    }
                }
                sample.setPosition(new Position(latitude, longitude));
                if (firstsample) {
                    lastPosition = sample.getPosition();
                    firstsample = false;
                }
                if (!distanceinsample) {
                    lastDistance += getDistanceFromPositions(lastPosition, sample.getPosition());
                    sample.setDistance((int) lastDistance);
                    lastPosition = sample.getPosition();
                }
                // Eliminates the jitters of 0bpm samples... assumes that heart rate won't change instantiously by much and
                // that there will only be the occasional missed heart beat.  Also fixes the laps not adding up.
                if (sample.getHeartRate() == 0)
                    sample.setHeartRate(lastSample.getHeartRate());
                else
                    lastSample.setHeartRate(sample.getHeartRate());
                exercise.getSampleList().add(sample);

                // update Zone information
                if (exercise.getHeartRateLimits() != null) {
                    for (int j = 0; j < 6; j++) {
                        if (sample.getHeartRate() > exercise.getHeartRateLimits().get(j).getUpperHeartRate()) {
                            aboveZone[j] += (currentOffset - lastOffset);
                        } else if (sample.getHeartRate() < exercise.getHeartRateLimits().get(j).getLowerHeartRate()) {
                            belowZone[j] += (currentOffset - lastOffset);
                        } else {
                            inZone[j] += (currentOffset - lastOffset);
                        }
                    }
                }
            }

        }

        // Store Zone Information in the exercise file
        if (exercise.getHeartRateLimits() != null) {
            for (int i = 0; i < 6; i++) {
                HeartRateLimit hrLimit = exercise.getHeartRateLimits().get(i);
                hrLimit.setTimeAbove((int) aboveZone[i]);
                hrLimit.setTimeBelow((int) belowZone[i]);
                hrLimit.setTimeWithin((int) inZone[i]);
            }
        }
        exercise.setRecordingInterval((short) 2);

        // some models (e.g. Timex Ironman Run Trainer) don't contain statistic date (avg, max, ...)
        // => compute the missing data   
        if (!exercise.getSampleList().isEmpty()) {
            computeHeartrateStatisticIfMissing(exercise);
            computeSpeedStatisticIfMissing(exercise);
            computeAltitudeStatisticIfMissing(exercise);
        }
        return exercise;
    }

    private Node findFirstPwx(Document doc) {
        // Find the first node of the document that is a pwx and then return it otherwise, return null
        // Normally only expect one node at this level but who knows.
        NodeList rootNodeList = doc.getChildNodes();
        for (int i = 0; i < rootNodeList.getLength(); i++) {
            if (rootNodeList.item(i).getNodeName().equals("pwx")) {
                return rootNodeList.item(i);
            }
        }
        return null;
    }

    @Override
    public EVExercise parseExercise(String filename) throws EVException {

        // create an EVExercise object from this data and set file type

        EVExercise exercise = new EVExercise(EVExercise.ExerciseFileType.TIMEX_PWX);
        // Open Document and Get root

        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        Document doc = null;

        Node root = null;

        NodeList children = null;
        // Open the pwx file
        try {
            dbf = DocumentBuilderFactory.newInstance(); // DocumentBuilderFactory
            db = dbf.newDocumentBuilder(); // DocumentBuilder
            doc = db.parse(filename); // Document
            root = findFirstPwx(doc); // Node
        } catch (Exception e) {
            throw new EVException("Failed to open pwx exercise file '" + filename + "' ...", e);
        }
        if (root != null)
            exercise.setFileType(EVExercise.ExerciseFileType.TIMEX_PWX);
        else
            throw new EVException("Failed to find a pwx node in file '" + filename + "'");

        children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals("workout")) {
                exercise = parseWorkoutNode(exercise, children.item(i));
            }
        }

        cleanupDistanceAndSpeedInSamples(exercise);

        // done :-) ?
        return exercise;
    }

    private void computeHeartrateStatisticIfMissing(EVExercise exercise) {
        if (exercise.getHeartRateAVG() == null) {
            double sumHeartrate = 0;

            for (ExerciseSample sample : exercise.getSampleList()) {
                sumHeartrate += sample.getHeartRate();
                short maxExerciseHeartrate = exercise.getHeartRateMax() == null ? 0 : exercise.getHeartRateMax();
                exercise.setHeartRateMax((short) Math.max(maxExerciseHeartrate, sample.getHeartRate()));
            }
            exercise.setHeartRateAVG((short) Math.round(sumHeartrate / (double) exercise.getSampleList().size()));
        }
    }

    private void computeSpeedStatisticIfMissing(EVExercise exercise) {
        if (exercise.getRecordingMode().isSpeed() && exercise.getSpeed() == null) {

            float speedMax = (float) exercise.getSampleList().stream()
                    .mapToDouble(sample -> sample.getSpeed() == null ? 0f : sample.getSpeed())
                    .max()
                    .orElse(0f);

            ExerciseSample lastSample = exercise.getSampleList().get(exercise.getSampleList().size() - 1);
            int distance = lastSample.getDistance();
            float speedAvg = (float) (CalculationUtils.calculateAvgSpeed(distance / 1000.0,
                    Math.round(exercise.getDuration() / 10f)));

            exercise.setSpeed(new ExerciseSpeed(speedAvg, speedMax, distance));
        }
    }

    private void computeAltitudeStatisticIfMissing(EVExercise exercise) {
        if (exercise.getRecordingMode().isAltitude() && exercise.getAltitude() == null) {

            short altitudeMin = Short.MAX_VALUE;
            short altitudeMax = Short.MIN_VALUE;
            int ascent = 0;
            double sumAltitude = 0;
            short previousAltitude = Short.MAX_VALUE;

            for (ExerciseSample sample : exercise.getSampleList()) {
                sumAltitude += sample.getAltitude();
                altitudeMin = (short) Math.min(altitudeMin, sample.getAltitude());
                altitudeMax = (short) Math.max(altitudeMax, sample.getAltitude());

                if (previousAltitude < sample.getAltitude()) {
                    ascent += sample.getAltitude() - previousAltitude;
                }
                previousAltitude = sample.getAltitude();
            }

            short altitudeAvg = (short) Math.round(sumAltitude / (double) exercise.getSampleList().size());
            exercise.setAltitude(new ExerciseAltitude(altitudeMin, altitudeAvg, altitudeMax, ascent, 0));
        }
    }

    private void cleanupDistanceAndSpeedInSamples(EVExercise exercise) {

        // when all sample contain the distance of 0 then set them to null
        // (for some models the distance is available for the laps only)
        boolean isDistanceInSamples = exercise.getSampleList().stream()
                .anyMatch(sample -> sample.getDistance() != null && sample.getDistance() > 0);
        if (!isDistanceInSamples) {
            exercise.getSampleList().stream().forEach(sample -> sample.setDistance(null));
        }

        // sometimes the speed data is missing in some samples only
        // (the speed of a samples can be null although other samples have speed data)
        // => set the speed of 0 instead of null for those samples
        boolean isSpeedInSamples = exercise.getSampleList().stream()
                .anyMatch(sample -> sample.getSpeed() != null && sample.getSpeed() > 0f);
        if (isSpeedInSamples) {
            exercise.getSampleList().stream().forEach(sample -> {
                if (sample.getSpeed() == null) {
                    sample.setSpeed(0f);
                }
            });
        }
    }
}

