package mobi.daogu.gaussblurstrict;

import android.content.Context;
import android.graphics.Bitmap;
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

    private RSCBlurH mRSCBlurH;
    private RSCBlurV mRSCBlurV;

    /** {@link Allocation} of input image data.*/
    private Allocation mAllocInput;

    /** {@link Allocation} of horizontal Gaussian blur.*/
    private Allocation mAllocHBlur;

    /** {@link Allocation} of vertical Gaussian blur.*/
    private Allocation mAllocVBlur;

    private Allocation mAllocEachLineH, mAllocEachLineV;

    private Allocation mAllocGuassKernel;

    private RenderScript mRS;

    /** Class of horizontal Gaussian blur filter.*/
    private final static class RSCBlurH extends ScriptC {
        private final static int mExportVarIdx_radiusX2 = 0;
        private final static int mExportVarIdx_pixn = 1;
        private final static int mExportVarIdx_gDst = 2;
        private final static int mExportVarIdx_gSrc = 3;
        private final static int mExportVarIdx_gGuass = 4;
        private final static int mExportForEachIdx_root = 0;

        public RSCBlurH(RenderScript rs, int radiusX2, int width,
                Allocation kernel, Allocation input, Allocation output) {
            super(rs, rs.getApplicationContext().getResources(),
                    rs.getApplicationContext().getResources().getIdentifier( "rsc_blur_h_expand",
                            "raw", rs.getApplicationContext().getPackageName()));
            setVar(mExportVarIdx_pixn, width);
            setVar(mExportVarIdx_radiusX2, radiusX2);
            bindAllocation(kernel, mExportVarIdx_gGuass);
            bindAllocation(input, mExportVarIdx_gSrc);
            bindAllocation(output, mExportVarIdx_gDst);
        }

        public void process(Allocation eachline) {
            forEach(mExportForEachIdx_root, eachline, null, null);
        }
    }

    /** Class of vertical Gaussian blur filter.*/
    private final static class RSCBlurV extends ScriptC {
        private final static int mExportVarIdx_radiusX2 = 0;
        private final static int mExportVarIdx_pixn = 1;
        private final static int mExportVarIdx_width = 2;
        private final static int mExportVarIdx_gDst = 3;
        private final static int mExportVarIdx_gSrc = 4;
        private final static int mExportVarIdx_gGuass = 5;
        private final static int mExportForEachIdx_root = 0;

        public RSCBlurV(RenderScript rs, int radiusX2, int width, int height,
                Allocation kernel, Allocation input, Allocation output) {
            super(rs, rs.getApplicationContext().getResources(),
                    rs.getApplicationContext().getResources().getIdentifier( "rsc_blur_v",
                            "raw", rs.getApplicationContext().getPackageName()));
            setVar(mExportVarIdx_pixn, height);
            setVar(mExportVarIdx_radiusX2, radiusX2);
            setVar(mExportVarIdx_width, width);
            bindAllocation(kernel, mExportVarIdx_gGuass);
            bindAllocation(input, mExportVarIdx_gSrc);
            bindAllocation(output, mExportVarIdx_gDst);
        }

        public void process(Allocation eachline) {
            forEach(mExportForEachIdx_root, eachline, null, null);
        }
    }

    /**
     * @param rs : Specific {@link RenderScript} for this {@link GaussBlurStrict} filter
     * @param radius : Radius of Gaussian blur.
     * @param width : Width of the input image.
     * @param height : Height of the input image.*/
    public GaussBlur(RenderScript rs, int radius, int width, int height) {
        int radiusX2 = radius * 2;
        mWidth = width;
        mHeight = height;
        mRetWidth = width - radius;
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
        mRetWidth = width - radius;
        mRetHeight = height - radiusX2;
        mRS = RenderScript.create(context);
        create(radius / 3f, radiusX2);
    }

    private void create(float sigma, int radiusX2) {
        int usage = Allocation.USAGE_SCRIPT;
        Type.Builder builder = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        Type tp_in = builder.setX(mWidth).setY(mHeight).create();
        Type tp_hb = builder.setX(mRetWidth).setY(mHeight).create();
        Type tp_vb = builder.setX(mRetWidth).setY(mRetHeight).create();

        mAllocInput = Allocation.createTyped(mRS, tp_in, usage);
        mAllocHBlur = Allocation.createTyped(mRS, tp_hb, usage);
        mAllocVBlur = Allocation.createTyped(mRS, tp_vb, usage);

        mAllocEachLineH = Allocation.createSized(mRS, Element.U16(mRS), mHeight, usage);
        mAllocEachLineV = Allocation.createSized(mRS, Element.U16(mRS), mRetWidth, usage);
        mAllocGuassKernel = generateGaussKernel(sigma, radiusX2);

        mRSCBlurH = new RSCBlurH(mRS, radiusX2, mWidth, mAllocGuassKernel, mAllocInput,
                mAllocHBlur);
        mRSCBlurV = new RSCBlurV(mRS, radiusX2, mRetWidth, mHeight, mAllocGuassKernel,
                mAllocHBlur, mAllocVBlur);

        short max = (short)(mRetWidth > mHeight ? mRetWidth : mHeight);
        short[] ids = new short[max];
        for (short i = 0; i < max; i++) ids[i] = i;
        mAllocEachLineH.copyFrom(ids);
        mAllocEachLineV.copyFrom(ids);
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
    public Bitmap generate(Bitmap input) throws Exception {
        if (input.getWidth() != mWidth || input.getHeight() != mHeight) {
            throw new Exception("Input size:" + input.getWidth() + "x" + input.getHeight()
                    + " not match. Expected is:" + mWidth + "x" + mHeight);
        }

        mAllocInput.copyFrom(input);

        mRSCBlurH.process(mAllocEachLineH);

        mRSCBlurV.process(mAllocEachLineV);

        Bitmap retBmp = Bitmap.createBitmap(mRetWidth, mRetHeight, Bitmap.Config.ARGB_8888);

        mAllocVBlur.copyTo(retBmp);

        return retBmp;
    }

    /** Release allocations and resources belong to this Gaussian blur filter.*/
    public void release() {
        mRS.destroy();
        mAllocHBlur.destroy();
        mAllocVBlur.destroy();
        mAllocInput.destroy();
        mAllocEachLineH.destroy();
        mAllocEachLineV.destroy();
        mAllocGuassKernel.destroy();
    }
}