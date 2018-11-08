//package com.hydra.services.tdcviewer
//
//import scalafx.animation.Timeline
//import scalafx.scene.chart.ValueAxis
//import scala.collection.JavaConverters._
//import scala.collection.mutable.ArrayBuffer
//import scalafx.beans.property.DoubleProperty
//
///**
//  * A logarithmic axis implementation for JavaFX 2 charts<br>
//  * <br>
//  *
//  * @author Kevin Senechal
//  */
//class LogarithmicAxis(lowerBound: Double, upperBound: Double) extends ValueAxis[Number](lowerBound, upperBound) {
//
//  /**
//    * The time of animation in ms
//    */
//  val ANIMATION_TIME = 2000.0
//  val lowerRangeTimeline = new Timeline()
//  val upperRangeTimeline = new Timeline()
//
//  val logUpperBound = DoubleProperty(0)
//  val logLowerBound = DoubleProperty(0)
//
//  validateBounds(lowerBound, upperBound)
//  bindLogBoundsToDefaultBounds()
//
//  /**
//    * Bind our logarithmic bounds with the super class bounds, consider the base 10 logarithmic scale.
//    */
//  def bindLogBoundsToDefaultBounds() {
//    logLowerBound.bind(new DoubleBinding() {
//      super.bind(lowerBoundProperty())
//
//      def computeValue(): Double = Math.log10(lowerBoundProperty().get())
//    })
//    logUpperBound.bind(new DoubleBinding() {
//      super.bind(upperBoundProperty())
//
//      def computeValue(): Double = Math.log10(upperBoundProperty().get())
//    })
//  }
//
//  /**
//    * Validate the bounds by throwing an exception if the values are not conform to the mathematics log interval:
//    * ]0,Double.MAX_VALUE]
//    *
//    * @param lowerBound
//    * @param upperBound
//    */
//  def validateBounds(lowerBound: Double, upperBound: Double) {
//    if (lowerBound < 0 || upperBound < 0 || lowerBound > upperBound) {
//      throw new IllegalArgumentException("The logarithmic range should be include to ]0,Double.MAX_VALUE] and the lowerBound should be less than the upperBound")
//    }
//  }
//
//  /**
//    * {@inheritDoc }
//    */
//  def calculateMinorTickMarks() = {
//    val range = getRange()
//    val minorTickMarksPositions = ArrayBuffer[Number]()
//    if (range != null) {
//      val upperBound = range(1)
//      val logUpperBound = Math.log10(upperBound.doubleValue())
//      val minorTickMarkCount = getMinorTickCount()
//      Range(0, logUpperBound.toInt + 1).foreach(i => Range(0, 9 * minorTickMarkCount + 1).foreach(jj => {
//        val j = jj / minorTickMarkCount
//        val value = j * Math.pow(10, i)
//        minorTickMarksPositions += value
//      }))
//    }
//    minorTickMarksPositions.toList.asJava
//  }
//
//  /**
//    * {@inheritDoc }
//    */
//  def calculateTickValues(length: Double, range: Any) = {
//    val tickPositions = new ArrayBuffer[Number]()
//    if (range != null) {
//      val rangeNA = range.asInstanceOf[Array[Number]]
//      val lowerBound = rangeNA(0)
//      val upperBound = rangeNA(1)
//      val logLowerBound = Math.log10(lowerBound.doubleValue())
//      val logUpperBound = Math.log10(upperBound.doubleValue())
//      Range(0, logLowerBound.toInt + 1).foreach(i => Range(1, 10).foreach(j => {
//        val value = j * Math.pow(10, i)
//        tickPositions += value
//      }))
//    }
//    tickPositions.toList
//  }
//
//  def getRange() = Array[Number](lowerBoundProperty().get(), upperBoundProperty().get())
//
//  //    @Override
//  //    protected String getTickMarkLabel(Number value) {
//  //        NumberFormat formatter = NumberFormat.getInstance();
//  //        formatter.setMaximumIntegerDigits(6);
//  //        formatter.setMinimumIntegerDigits(1);
//  //        return formatter.format(value);
//  //    }
//  //
//  //    /**
//  //     * {@inheritDoc}
//  //     */
//  //    @Override
//  //    protected void setRange(Object range, boolean animate) {
//  //        if (range != null) {
//  //            Number lowerBound = ((Number[]) range)[0];
//  //            Number upperBound = ((Number[]) range)[1];
//  //            try {
//  //                validateBounds(lowerBound.doubleValue(), upperBound.doubleValue());
//  //            } catch (IllegalArgumentException e) {
//  //                e.printStackTrace();
//  //            }
//  //            if (animate) {
//  //                try {
//  //                    lowerRangeTimeline.getKeyFrames().clear();
//  //                    upperRangeTimeline.getKeyFrames().clear();
//  //
//  //                    lowerRangeTimeline.getKeyFrames()
//  //                            .addAll(new KeyFrame(Duration.ZERO, new KeyValue(lowerBoundProperty(), lowerBoundProperty()
//  //                                            .get())),
//  //                                    new KeyFrame(new Duration(ANIMATION_TIME), new KeyValue(lowerBoundProperty(),
//  //                                            lowerBound.doubleValue())));
//  //
//  //                    upperRangeTimeline.getKeyFrames()
//  //                            .addAll(new KeyFrame(Duration.ZERO, new KeyValue(upperBoundProperty(), upperBoundProperty()
//  //                                            .get())),
//  //                                    new KeyFrame(new Duration(ANIMATION_TIME), new KeyValue(upperBoundProperty(),
//  //                                            upperBound.doubleValue())));
//  //                    lowerRangeTimeline.play();
//  //                    upperRangeTimeline.play();
//  //                } catch (Exception e) {
//  //                    lowerBoundProperty().set(lowerBound.doubleValue());
//  //                    upperBoundProperty().set(upperBound.doubleValue());
//  //                }
//  //            }
//  //            lowerBoundProperty().set(lowerBound.doubleValue());
//  //            upperBoundProperty().set(upperBound.doubleValue());
//  //        }
//  //    }
//  //
//  //    @Override
//  //    public Number getValueForDisplay(double displayPosition) {
//  //        double delta = logUpperBound.get() - logLowerBound.get();
//  //        if (getSide().isVertical()) {
//  //            return Math.pow(10, (((displayPosition - getHeight()) / -getHeight()) * delta) + logLowerBound.get());
//  //        } else {
//  //            return Math.pow(10, (((displayPosition / getWidth()) * delta) + logLowerBound.get()));
//  //        }
//  //    }
//  //
//  //    @Override
//  //    public double getDisplayPosition(Number value) {
//  //        double delta = logUpperBound.get() - logLowerBound.get();
//  //        double deltaV = Math.log10(value.doubleValue()) - logLowerBound.get();
//  //        if (getSide().isVertical()) {
//  //            return (1. - ((deltaV) / delta)) * getHeight();
//  //        } else {
//  //            return ((deltaV) / delta) * getWidth();
//  //        }
//  //    }
//}