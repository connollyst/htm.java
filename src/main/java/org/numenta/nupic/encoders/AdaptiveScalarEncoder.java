package org.numenta.nupic.encoders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdaptiveScalarEncoder extends ScalarEncoder {

	/*
	 * This is an implementation of the scalar encoder that adapts the min and
	 * max of the scalar encoder dynamically. This is essential to the streaming
	 * model of the online prediction framework.
	 * 
	 * Initialization of an adapive encoder using resolution or radius is not
	 * supported; it must be intitialized with n. This n is kept constant while
	 * the min and max of the encoder changes.
	 * 
	 * The adaptive encoder must be have periodic set to false.
	 * 
	 * The adaptive encoder may be initialized with a minval and maxval or with
	 * `None` for each of these. In the latter case, the min and max are set as
	 * the 1st and 99th percentile over a window of the past 100 records.
	 * 
	 * *Note:** the sliding window may record duplicates of the values in the
	 * dataset, and therefore does not reflect the statistical distribution of
	 * the input data and may not be used to calculate the median, mean etc.
	 */
	
	public int recordNum = 0;
	public boolean learningEnabled = true;
	public Double[] slidingWindow = new Double[0];
	public int windowSize = 300;
	public Double bucketValues;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.numenta.nupic.encoders.ScalarEncoder#init()
	 */
	@Override
	public void init() {
		this.setPeriodic(false);
		super.init();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.numenta.nupic.encoders.ScalarEncoder#initEncoder(int, double,
	 * double, int, double, double)
	 */
	@Override
	public void initEncoder(int w, double minVal, double maxVal, int n,
			double radius, double resolution) {
		this.setPeriodic(false);
		this.encLearningEnabled = true;
		if (this.periodic) {
			throw new IllegalStateException(
					"Adaptive scalar encoder does not encode periodic inputs");
		}
		assert n != 0;
		super.initEncoder(w, minVal, maxVal, n, radius, resolution);
	}

	/**
	 *
	 */
	public AdaptiveScalarEncoder() {
	}

	/**
	 * Returns a builder for building AdaptiveScalarEncoder. This builder may be
	 * reused to produce multiple builders
	 * 
	 * @return a {@code AdaptiveScalarEncoder.Builder}
	 */
	public static AdaptiveScalarEncoder.Builder adaptiveBuilder() {
		return new AdaptiveScalarEncoder.Builder();
	}

	public static class Builder extends Encoder.Builder<AdaptiveScalarEncoder.Builder, AdaptiveScalarEncoder> {
		private Builder() {}

		@Override
		public AdaptiveScalarEncoder build() {
			encoder = new AdaptiveScalarEncoder();
			super.build();
			((AdaptiveScalarEncoder) encoder).init();
			return (AdaptiveScalarEncoder) encoder;
		}
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.ScalarEncoder#topDownCompute(int[])
	 */
	@Override
	public List<EncoderResult> topDownCompute(int[] encoded) {
		if (this.getMinVal() == 0 || this.getMaxVal() == 0) {
			List<EncoderResult> res = new ArrayList<EncoderResult>();
			int[] enArray = new int[this.getN()];
			Arrays.fill(enArray, 0);
			EncoderResult ecResult = new EncoderResult(0, 0, enArray);
			res.add(ecResult);
			return res;
		}
		return super.topDownCompute(encoded);
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.ScalarEncoder#encodeIntoArray(java.lang.Double, int[])
	 */
	@Override
	public void encodeIntoArray(Double input, int[] output) {
		this.recordNum += 1;
		boolean learn = false;
		if (!this.encLearningEnabled) {
			learn = true;
		}
		if (input == AdaptiveScalarEncoder.SENTINEL_VALUE_FOR_MISSING_DATA) {
			Arrays.fill(output, 0);
		} else if (!Double.isNaN(input)) {
			this.setMinAndMax(input, learn);
		}
		super.encodeIntoArray(input, output);
	}

	private void setMinAndMax(Double input, boolean learn) {
		if (slidingWindow.length >= windowSize) {
			slidingWindow = deleteItem(slidingWindow, 0);
		}
		slidingWindow = appendItem(slidingWindow, input);
		
		if (this.minVal == this.maxVal) {
			this.minVal = input;
			this.maxVal = input + 1;
			setEncoderParams();
		} else {
			Double[] sorted = Arrays.copyOf(slidingWindow, slidingWindow.length);
			Arrays.sort(sorted);
			double minOverWindow = sorted[0];
			double maxOverWindow = sorted[sorted.length - 1];
			if (minOverWindow < this.minVal) {
				if (this.verbosity >= 2) {
					System.out.println(String.format("Input %s=%d smaller than minval %d. Adjusting minval to %d",
							this.name, input, this.minVal, minOverWindow));
					this.minVal = minOverWindow;
					setEncoderParams();
				}
			}
			if (maxOverWindow > this.maxVal) {
				if (this.verbosity >= 2) {
					System.out.println(String.format("Input %s=%d greater than maxval %d. Adjusting maxval to %d",
							this.name, input, this.minVal, minOverWindow));
					this.maxVal = maxOverWindow;
					setEncoderParams();
				}
			}
		}
	}

	private void setEncoderParams() {
		this.rangeInternal = this.maxVal - this.minVal;
		this.resolution = this.rangeInternal / (this.n - this.w);
		this.radius = this.w * this.resolution;
		this.range = this.rangeInternal + this.resolution;
		this.nInternal = this.n - 2 * this.padding;
		this.bucketValues = null;
	}

	private Double[] appendItem(Double[] a, Double input) {
		a = Arrays.copyOf(a, a.length + 1);
		a[a.length - 1] = input;
		return a;
	}

	private Double[] deleteItem(Double[] a, int i) {
		a = Arrays.copyOfRange(a, 1, a.length - 1);
		return a;
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.ScalarEncoder#getBucketIndices(java.lang.String)
	 */
	@Override
	public int[] getBucketIndices(String inputString) {
		double input = Double.parseDouble(inputString);
		return calculateBucketIndices(input);
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.ScalarEncoder#getBucketIndices(double)
	 */
	@Override
	public int[] getBucketIndices(double input) {
		return calculateBucketIndices(input);
	}

	private int[] calculateBucketIndices(double input) {
		this.recordNum += 1;
		boolean learn = false;
		if (!this.encLearningEnabled) {
			learn = true;
		}
		if ((Double.isNaN(input)) && (Double.valueOf(input) instanceof Double)) {
			input = AdaptiveScalarEncoder.SENTINEL_VALUE_FOR_MISSING_DATA;
		}
		if (input == AdaptiveScalarEncoder.SENTINEL_VALUE_FOR_MISSING_DATA) {
			return new int[this.n];
		} else {
			this.setMinAndMax(input, learn);
		}
		return super.getBucketIndices(input);
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.ScalarEncoder#getBucketInfo(int[])
	 */
	@Override
	public List<EncoderResult> getBucketInfo(int[] buckets) {
		if (this.minVal == 0 || this.maxVal == 0) {
			int[] initialBuckets = new int[this.n];
			Arrays.fill(initialBuckets, 0);
			List<EncoderResult> encoderResultList = new ArrayList<EncoderResult>();
			EncoderResult encoderResult = new EncoderResult(0, 0, initialBuckets);
			encoderResultList.add(encoderResult);
			return encoderResultList;
		}
		return super.getBucketInfo(buckets);
	}
}
