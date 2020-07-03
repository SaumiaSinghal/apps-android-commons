package fr.free.nrw.commons.media;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import androidx.annotation.Nullable;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatCheckerUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;

public class CustomImageDecoder implements ImageDecoder {

  public static final ImageFormat SVG_FORMAT = new ImageFormat("SVG_FORMAT", "svg");
  private static final String HEADER_TAG = "<svg";

  private static final byte[][] POSSIBLE_HEADER_TAGS = {
      ImageFormatCheckerUtils.asciiBytes("<?xml")
  };

  @Override
  public CloseableImage decode(EncodedImage encodedImage, int length, QualityInfo qualityInfo,
      ImageDecodeOptions options) {
      try {
        SVG svg = SVG.getFromInputStream(encodedImage.getInputStream());
        return new CloseableSvgImage(svg);
      } catch (SVGParseException e) {
        e.printStackTrace();
      }
      return null;
  }

  public static class SvgFormatChecker implements ImageFormat.FormatChecker {

    public static final byte[] HEADER = ImageFormatCheckerUtils.asciiBytes(HEADER_TAG);

    @Override
    public int getHeaderSize() {
      return HEADER.length;
    }

    @Nullable
    @Override
    public ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
      if (headerSize < getHeaderSize()) {
        return null;
      }
      if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, HEADER)) {
        return SVG_FORMAT;
      }
      for (byte[] possibleHeaderTag : POSSIBLE_HEADER_TAGS) {
        if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, possibleHeaderTag)
            && ImageFormatCheckerUtils.indexOfPattern(
            headerBytes, headerBytes.length, HEADER, HEADER.length)
            > -1) {
          return SVG_FORMAT;
        }
      }
      return null;
    }
  }

  /** SVG drawable factory that creates {@link PictureDrawable}s for SVG images. */
  public static class SvgDrawableFactory implements DrawableFactory {

    static int width;
    static int height;

    @Override
    public boolean supportsImageType(CloseableImage image) {
      return image instanceof CloseableSvgImage;
    }

    @Nullable
    @Override
    public Drawable createDrawable(CloseableImage image) {
      return new SvgPictureDrawable(((CloseableSvgImage) image).getSvg());
    }
  }

  public static class SvgPictureDrawable extends PictureDrawable {

    private final SVG mSvg;
    public static int imagewidth;
    public static int imageheight;

    public SvgPictureDrawable(SVG svg) {
      super(null);
      mSvg = svg;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      super.onBoundsChange(bounds);
      imageheight = bounds.height();
      imagewidth = bounds.width();
      setPicture(mSvg.renderToPicture(bounds.width(), bounds.height()));
    }
  }

  public static class CloseableSvgImage extends CloseableImage {

    private final SVG mSvg;

    private boolean mClosed = false;

    public CloseableSvgImage(SVG svg) {
      mSvg = svg;
    }

    public SVG getSvg() {
      return mSvg;
    }

    @Override
    public int getSizeInBytes() {
      return 0;
    }

    @Override
    public void close() {
      mClosed = true;
    }

    @Override
    public boolean isClosed() {
      return mClosed;
    }

    @Override
    public int getWidth() {
      return SvgPictureDrawable.imagewidth;
    }

    @Override
    public int getHeight() {
      return SvgPictureDrawable.imageheight;
    }
  }
}
