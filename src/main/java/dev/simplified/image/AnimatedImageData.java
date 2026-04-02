package dev.sbs.api.io.image;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.builder.BuildFlag;
import dev.sbs.api.util.builder.ClassBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A multi-frame animated image with timing, loop control, and background color metadata.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AnimatedImageData implements ImageData {

    private final int width;
    private final int height;
    @Getter(AccessLevel.NONE)
    private final boolean alpha;
    private final @NotNull ConcurrentList<ImageFrame> frames;
    private final int loopCount;
    private final int backgroundColor;

    @Override
    public boolean hasAlpha() {
        return this.alpha;
    }

    @Override
    public boolean isAnimated() {
        return true;
    }

    /**
     * Returns a new builder for constructing animated image data.
     *
     * @return a new builder instance
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builds {@link AnimatedImageData} instances with configurable animation parameters.
     */
    public static class Builder implements ClassBuilder<AnimatedImageData> {

        @BuildFlag(nonNull = true, notEmpty = true)
        private ConcurrentList<ImageFrame> frames = Concurrent.newList();
        private int width = -1;
        private int height = -1;
        private int loopCount = 0;
        private int backgroundColor = 0;

        /**
         * Appends a frame to the animation sequence.
         *
         * @param frame the frame to add
         * @return this builder for chaining
         */
        public @NotNull Builder withFrame(@NotNull ImageFrame frame) {
            this.frames.add(frame);
            return this;
        }

        /**
         * Replaces the frame list with the given frames.
         *
         * @param frames the frames to use
         * @return this builder for chaining
         */
        public @NotNull Builder withFrames(@NotNull ConcurrentList<ImageFrame> frames) {
            this.frames = frames;
            return this;
        }

        /**
         * Sets the canvas width explicitly.
         * <p>
         * When not set, the width is derived from the first frame.
         *
         * @param width the canvas width in pixels
         * @return this builder for chaining
         */
        public @NotNull Builder withWidth(int width) {
            this.width = width;
            return this;
        }

        /**
         * Sets the canvas height explicitly.
         * <p>
         * When not set, the height is derived from the first frame.
         *
         * @param height the canvas height in pixels
         * @return this builder for chaining
         */
        public @NotNull Builder withHeight(int height) {
            this.height = height;
            return this;
        }

        /**
         * Sets the animation loop count.
         *
         * @param loopCount the number of times to loop (0 for infinite)
         * @return this builder for chaining
         */
        public @NotNull Builder withLoopCount(int loopCount) {
            this.loopCount = loopCount;
            return this;
        }

        /**
         * Sets the canvas background color as a packed ARGB integer.
         *
         * @param backgroundColor the ARGB background color
         * @return this builder for chaining
         */
        public @NotNull Builder withBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        @Override
        public @NotNull AnimatedImageData build() {
            ImageFrame first = this.frames.getFirst();
            int w = this.width > 0 ? this.width : first.getImage().getWidth();
            int h = this.height > 0 ? this.height : first.getImage().getHeight();
            boolean alpha = first.getImage().getColorModel().hasAlpha();

            return new AnimatedImageData(w, h, alpha, this.frames, this.loopCount, this.backgroundColor);
        }

    }

}
