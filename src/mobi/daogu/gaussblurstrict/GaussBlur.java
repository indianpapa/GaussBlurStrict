package mobi.daogu.gaussblurstrict;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;
import android.renderscript.Type;

/**Class of Gaussian blur filter. Applies a gaussian blur of the specified radius to a
 * given bitmap.
 * @author daogu.mobi@gmail.com */
public final class GaussBlur {
    private int mWidth, mHeight, mRetWidth, mRetHeight;

    private RSCBlurHStrict mRSCBlurHS;
    private RSCBlurVStrict mRSCBlurVS;

    /** {@link Allocation} of input image data.*/
    private Allocation mAllocInput;

    /** {@link Allocation} of horizontal Gaussian blur.*/
    private Allocation mAllocHBlur;

    /** {@link Allocation} of horizontal Gaussian blur with edge cut-off.*/
    private Allocation mAllocHBlurS;

    /** {@link Allocation} of vertical Gaussian blur with edge cut-off.*/
    private Allocation mAllocVBlurS;

    private Allocation mAllocEachLineH, mAllocEachLineV, mAllocEachLineVS;

    private Allocation mAllocGuassKernel;

    private RenderScript mRS;

    private static int getAlignedPixn(int num) {
        return (int)Math.ceil(num / 4.0) * 4;
    }

    /** Class of horizontal Gaussian blur filter with edge cut-off.*/
    private final static class RSCBlurHStrict extends ScriptC {
        private final static int mExportVarIdx_radiusX2 = 0;
        private final static int mExportVarIdx_dstlen = 1;
        private final static int mExportVarIdx_srclen = 2;
        private final static int mExportVarIdx_gDst = 3;
        private final static int mExportVarIdx_gSrc = 4;
        private final static int mExportVarIdx_gGuass = 5;
        private final static int mExportForEachIdx_root = 0;

        public RSCBlurHStrict(RenderScript rs, int radiusX2, int width,
                Allocation kernel, Allocation input, Allocation output) {
            super(rs, rs.getApplicationContext().getResources(),
                    rs.getApplicationContext().getResources().getIdentifier( "rsc_blur_h",
                            "raw", rs.getApplicationContext().getPackageName()));
            if (Build.VERSION.SDK_INT > 18) {
                setVar(mExportVarIdx_dstlen, getAlignedPixn(width - radiusX2));
                setVar(mExportVarIdx_srclen, getAlignedPixn(width));
            } else {
                setVar(mExportVarIdx_dstlen, width - radiusX2);
                setVar(mExportVarIdx_srclen, width);
            }
            setVar(mExportVarIdx_radiusX2, radiusX2);
            bindAllocation(kernel, mExportVarIdx_gGuass);
            bindAllocation(input, mExportVarIdx_gSrc);
            bindAllocation(output, mExportVarIdx_gDst);
        }

        public void process(Allocation eachline) {
            forEach(mExportForEachIdx_root, eachline, null, null);
        }
    }

    /** Class of vertical Gaussian blur filter with edge cut-off.*/
    private final static class RSCBlurVStrict extends ScriptC {
        private final static int mExportVarIdx_radiusX2 = 0;
        private final static int mExportVarIdx_dstlen = 1;
        private final static int mExportVarIdx_srclen = 2;
        private final static int mExportVarIdx_width = 3;
        private final static int mExportVarIdx_gDst = 4;
        private final static int mExportVarIdx_gSrc = 5;
        private final static int mExportVarIdx_gGuass = 6;
        private final static int mExportForEachIdx_root = 0;

        public RSCBlurVStrict(RenderScript rs, int radiusX2, int width, int height,
                Allocation kernel, Allocation input, Allocation output) {
            super(rs, rs.getApplicationContext().getResources(),
                    rs.getApplicationContext().getResources().getIdentifier( "rsc_blur_v",
                            "raw", rs.getApplicationContext().getPackageName()));
            setVar(mExportVarIdx_dstlen, height - radiusX2);
            setVar(mExportVarIdx_srclen, height);
            setVar(mExportVarIdx_radiusX2, radiusX2);
            if (Build.VERSION.SDK_INT > 18) {
                setVar(mExportVarIdx_width, getAlignedPixn(width));
            } else {
                setVar(mExportVarIdx_width, width);
            }
            bindAllocation(kernel, mExportVarIdx_gGuass);
            bindAllocation(input, mExportVarIdx_gSrc);
            bindAllocation(output, mExportVarIdx_gDst);
        }

        public void process(Allocation eachline) {
            forEach(mExportForEachIdx_root, eachline, null, null);
        }
    }

    /**
     * @param rs : Specific {@link RenderScript} for this {@link GaussBlur} filter
     * @param radius : Radius of Gaussian blur.
     * @param width : Width of the input image.
     * @param height : Height of the input image.*/
    public GaussBlur(RenderScript rs, int radius, int width, int height) {
        int radiusX2 = radius * 2;
        mWidth = width;
        mHeight = height;
        mRetWidth = width - radiusX2;
        mRetHeight = height - radiusX2;
        mRS = rs;
        create(radius / 3f, radiusX2);
    }

    /**
     * @param context : The specific {@link Context} for creating {@link RenderScript}.
     * @param radius : Radius of Gaussian blur.
     * @param width : Width of the input image.
     * @param height : Height of the input image.*/
    public GaussBlur(Context context, int radius, int width, int height) {
        int radiusX2 = radius * 2;
        mWidth = width;
        mHeight = height;
        mRetWidth = width - radiusX2;
        mRetHeight = height - radiusX2;;
        mRS = RenderScript.create(context);
        create(radius / 3f, radiusX2);
    }

    public RenderScript getRenderScript() {
        return mRS;
    }

    private void create(float sigma, int radiusX2) {
        int usage = Allocation.USAGE_SCRIPT;
        Type.Builder builder = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        Type tp_in = builder.setX(mWidth).setY(mHeight).create();
        Type tp_hb = builder.setX(mRetWidth).setY(mHeight).create();
        Type tp_vb = builder.setX(mRetWidth).setY(mRetHeight).create();

        mAllocInput = Allocation.createTyped(mRS, tp_in, usage);
        mAllocHBlur = Allocation.createTyped(mRS, tp_in, usage);
        mAllocHBlurS = Allocation.createTyped(mRS, tp_hb, usage);
        mAllocVBlurS = Allocation.createTyped(mRS, tp_vb, usage);

        mAllocEachLineH = Allocation.createSized(mRS, Element.U16(mRS), mHeight, usage);
        mAllocEachLineV = Allocation.createSized(mRS, Element.U16(mRS), mWidth, usage);
        mAllocEachLineVS = Allocation.createSized(mRS, Element.U16(mRS), mRetWidth, usage);
        mAllocGuassKernel = generateGaussKernel(sigma, radiusX2);

        mRSCBlurHS = new RSCBlurHStrict(mRS, radiusX2, mWidth, mAllocGuassKernel, mAllocInput,
                mAllocHBlurS);
        mRSCBlurVS = new RSCBlurVStrict(mRS, radiusX2, mRetWidth, mHeight, mAllocGuassKernel,
                mAllocHBlurS, mAllocVBlurS);

        short max = (short)(mWidth > mHeight ? mWidth : mHeight);
        short[] ids = new short[max];
        for (short i = 0; i < max; i++) ids[i] = i;
        mAllocEachLineH.copyFrom(ids);
        mAllocEachLineV.copyFrom(ids);
        mAllocEachLineVS.copyFrom(ids);
    }

    private Allocation generateGaussKernel(float sigma, int radiusX2) {
        int len = radiusX2 + 1, i, j, radius = radiusX2 / 2;
        Allocation kernel = Allocation.createSized(mRS, Element.F32(mRS), len,
                Allocation.USAGE_SCRIPT);
        float sum = 0;
        double value;
        float[] values = new float[len];
        for (i = -radius, j = 0; i <= radius; i++, j++) {
            value = i / sigma;
            values[j] = (float)Math.exp(-0.5 * value * value);
            sum += values[j];
        }
        for (i = 0; i < len; i++) values[i] /= sum;
        kernel.copyFrom(values);
        return kernel;
    }

    /** Generate a blurred bitmap image with a given input bitmap image.
     * @param input : The given bitmap image.
     * @return The blurred bitmap.
     * @throws Exception */
    public Bitmap generateStrict(Bitmap input) throws Exception {
        if (input.getWidth() != mWidth || input.getHeight() != mHeight) {
            throw new Exception("Input size not match. Expected is:" + mWidth + "x" + mHeight);
        }
        mAllocInput.copyFrom(input);
        mRSCBlurHS.process(mAllocEachLineH);
        mRSCBlurVS.process(mAllocEachLineVS);
        Bitmap retBmp = Bitmap.createBitmap(mRetWidth, mRetHeight, Bitmap.Config.ARGB_8888);
        mAllocVBlurS.copyTo(retBmp);
        return retBmp;
    }

    /** Apply Gaussian blur on the input bitmap.
     * @param input : The given bitmap image.
     * @return True if successfully applied the Gaussian blur, otherwise false.*/
    public boolean apply(Bitmap input) {
        return true;
    }

    /** Release allocations and resources belong to this Gaussian blur filter.*/
    public void release() {
        mRS.destroy();
        mAllocInput.destroy();
        mAllocHBlur.destroy();
        mAllocHBlurS.destroy();
        mAllocVBlurS.destroy();
        mAllocEachLineH.destroy();
        mAllocEachLineV.destroy();
        mAllocEachLineVS.destroy();
        mAllocGuassKernel.destroy();
    }
}
