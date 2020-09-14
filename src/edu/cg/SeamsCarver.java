package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class SeamsCarver extends ImageProcessor {

	// MARK: An inner interface for functional programming.
	@FunctionalInterface
	interface ResizeOperation {
		BufferedImage resize();
	}

	// MARK: Fields
	private int numOfSeams;
	private ResizeOperation resizeOp;
	boolean[][] imageMask;
	long [][] cost;
	BufferedImage greyScaled;
	int[][] greyArray;
	int[][] seamsMapping;
	boolean[][] map;
	int[][] parent;
	int[][] energyMatrix;
	boolean[][] maskShift;
	boolean[][] increasedMask;
	BufferedImage originlImage;

	public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
			boolean[][] imageMask) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight());

		numOfSeams = Math.abs(outWidth - inWidth);
		this.imageMask = imageMask;
		if (inWidth < 2 | inHeight < 2)
			throw new RuntimeException("Can not apply seam carving: workingImage is too small");

		if (numOfSeams > inWidth / 2)
			throw new RuntimeException("Can not apply seam carving: too many seams...");

		// Setting resizeOp by with the appropriate method reference
		if (outWidth > inWidth)
			resizeOp = this::increaseImageWidth;
		else if (outWidth < inWidth)
			resizeOp = this::reduceImageWidth;
		else
			resizeOp = this::duplicateWorkingImage;
        maskShift = imageMask;
		seamsMapping = new int[inHeight][inWidth];
		for (int i = 0; i < inHeight; i++) {
			for (int j = 0; j < inWidth; j++) {
				seamsMapping[i][j] = j;
			}
		}
		energyMatrix = new int[inHeight][inWidth];
		greyScaled = this.greyscale();
		greyArray = getColorFromGreyscale();
		originlImage = workingImage;

		this.logger.log("preliminary calculations were ended.");
	}

	public BufferedImage resize() {
		return resizeOp.resize();
	}

	private BufferedImage reduceImageWidth() {
		logger.log("Starting Width reducing");
		int numberOfSeams = inWidth - outWidth;
		for (int k = 0; k <= numberOfSeams - 1; k++) {
                parent = new int[inHeight][inWidth-k];
                cost = new long[inHeight][inWidth-k];
                createEnergyMatrix();
		        calculateCostMatrix(k);
                findPath(k);
                removeSeam(k);
		}
		BufferedImage output = finalImg();
		return output;
	}

	private void calculateCostMatrix(int seamNum) {
        setForEachWidth(inWidth-seamNum);
        map = new boolean[inHeight][inWidth-seamNum];
        forEach((y,x) -> {
            if (y > 0) {
                long c_Value = calcCost(x, y, seamNum);
                cost[y][x] = energyMatrix[y][x] + c_Value;
            } else {
                cost[y][x] = energyMatrix[y][x];
            }

        });
    }

	private BufferedImage finalImg() {
		logger.log("Getting final image");
		BufferedImage finalImg = new BufferedImage(outWidth, outHeight, workingImageType);
		setForEachOutputParameters();
		forEach((y, x) -> {
			int c = workingImage.getRGB(seamsMapping[y][x], y);
			finalImg.setRGB(x, y, c);
		});
		return finalImg;
	}

	private void removeSeam(int seamNum) {
        logger.log("Removing Seam");
		boolean seenSeam = false;
		int[][] newSeamsMapping = new int[inHeight][map[0].length-1];
		int[][] newGreyArray = new int[inHeight][map[0].length-1];
		for (int i = 0; i < inHeight; i++) {
		    seenSeam = false;
			for (int j = 0; j < inWidth - seamNum - 1; j++) {
				if (seenSeam == false) {
					if (map[i][j] == true) {
						newSeamsMapping[i][j] = seamsMapping[i][j+1];
						newGreyArray[i][j] = greyArray[i][j+1];
						maskShift[i][j] = maskShift[i][j+1];
					    seenSeam = true;
					} else {
						newSeamsMapping[i][j] = seamsMapping[i][j];
                        newGreyArray[i][j] = greyArray[i][j];
                        maskShift[i][j] = maskShift[i][j];
					}
				} else {
					newSeamsMapping[i][j] = seamsMapping[i][j+1];
                    newGreyArray[i][j] = greyArray[i][j+1];
                    maskShift[i][j] = maskShift[i][j+1];
				}
			}
		}
		map = new boolean[inHeight][map[0].length-1];
		seamsMapping = newSeamsMapping;
		greyArray = newGreyArray;
	}

	private void findPath(int seamNum) {
        logger.log("Finding Cheapest Path");
        int minIndex = 0;
        long minValue = cost[cost.length-1][0];
        for (int j = 1; j < cost[0].length; j++) {
            if (cost[cost.length - 1][j] <= minValue) {
                minIndex = j;
                minValue = cost[cost.length - 1][j];
            }
        }
        //System.out.println("minimum index is: " + minIndex);
        map[cost.length - 1][minIndex] = true;
        for (int i = cost.length - 1; i > 0; i--) {
            map[i-1][parent[i][minIndex]] = true;
            minIndex = parent[i][minIndex];

        }
        //removeSeam(minIndex, seamNum);
    }
	private long calcCost(int x, int y, int seamNum) {
		int width = inWidth - seamNum;
		int minIndex = x;
		long p = 255L;
		long ml;
		long mr;
		long mv;
		long cl;
		long cr;
		long cv;

		mv = cost[y - 1][minIndex];
		if (minIndex > 0 && minIndex + 1 < width) {
			cl = Math.abs(greyArray[y][minIndex - 1] - greyArray[y][minIndex + 1]);
			cv = Math.abs(greyArray[y][minIndex - 1] - greyArray[y][minIndex + 1]);
			cr = Math.abs(greyArray[y][minIndex - 1] - greyArray[y][minIndex + 1]);
		} else {
			cl = 0L;
			cr = 0L;
			cv = p;
		}
		if (minIndex > 0) {
			cl += Math.abs(greyArray[y][minIndex - 1] - greyArray[y - 1][minIndex]);
			ml = cost[y - 1][minIndex - 1];

		} else {
			cl = 2147483647L;
			ml = 0L;
		}
		if (minIndex + 1 < width) {
			cr += Math.abs(greyArray[y][minIndex + 1] - greyArray[y - 1][minIndex]);
			mr = cost[y - 1][minIndex + 1];
		} else {
			cr = 2147483647L;
			mr = 0L;
		}

        cl = ml + cl;
        cv = mv + cv;
        cr = mr + cr;
		long min;
		if (cl < cr && cl < cv) {
			//minIndex = minIndex - 1;
            parent[y][minIndex] = minIndex - 1;
			min = cl;
		} else if (cr < cl && cr < cv) {
            parent[y][minIndex] = minIndex + 1;
			min = cr;
		}
		else {
            parent[y][minIndex] = minIndex;
			min = cv;
		}
		return min;
	}

    private void createEnergyMatrix() {
        logger.log("Getting energy");
        logger.log("Starting energy calc");

        this.energyMatrix = new int[inHeight][greyArray[0].length];


        int e1, e2, e3;
        for (int i = 0; i < greyArray.length ; i++) {
            for (int j = 0; j < greyArray[0].length ; j++) {
                if (j != greyArray[0].length-1) {
                    e1 = Math.abs(greyArray[i][j] - greyArray[i][j + 1]);
                } else {
                    e1 = Math.abs(greyArray[i][j] - greyArray[i][j - 1]);
                }
                if (i != greyArray.length-1) {
                    e2 = Math.abs(greyArray[i][j] - greyArray[i + 1][j]);
                } else {
                    e2 = Math.abs(greyArray[i][j] - greyArray[i - 1][j]);
                }
                if (this.imageMask[i][j] == true) {
                    e3 = Integer.MIN_VALUE;
                } else {
                    e3 = 0;
                }
                energyMatrix[i][j] = e1 + e2 + e3;
            }
        }

    }

	private int[][] getColorFromGreyscale() {
		logger.log("Getting grey scaled array");
		int[][] greyArray = new int[this.greyScaled.getHeight()][this.greyScaled.getWidth()];
		setForEachInputParameters();
		this.forEach((y,x) -> {
			Color c = new Color(greyScaled.getRGB(x, y));
			greyArray[y][x] = c.getBlue();
		});
		return greyArray;
	}



	public BufferedImage showSeams(int seamColorRGB) {
        BufferedImage seams = duplicateWorkingImage();
        int[][] fullSeamMapping = new int[inHeight][inWidth];
        if (inWidth > outWidth) {
			reduceImageWidth();
		} else {
        	increaseImageWidth();
		}
        for (int i = 0; i < inHeight; i++) {
            int currIndex = 0;
            for (int j = 0; j < inWidth; j++){
                if (currIndex == seamsMapping[i].length){
                    fullSeamMapping[i][j] = -1;
                } else if (seamsMapping[i][currIndex] > j) {
                    fullSeamMapping[i][j] = -1;
                } else if (seamsMapping[i][currIndex]== j){
                    fullSeamMapping[i][j] = j;
                    currIndex++;
                } else {
                    System.out.println("error");
                }
            }
        }
        System.out.println("length: " + seamsMapping.length);
        System.out.println("length: " + seamsMapping[0].length);
        setForEachParameters(inWidth, inHeight);
        forEach((y, x) -> {
            if (fullSeamMapping[y][x] == -1) {
                seams.setRGB(x, y, seamColorRGB);
            }
        });
        System.out.println("Done");
        return seams;
	}

	public boolean[][] getMaskAfterSeamCarving() {
		System.out.println("im in getMaskAfterSeamCarving");
		boolean[][] newMask;
		if (outWidth < inWidth) {
			newMask = new boolean[outHeight][outWidth];
			setForEachOutputParameters();
			forEach((y, x) -> {
				newMask[y][x] = maskShift[y][x];
			});
		}
		else if(outWidth == inWidth) {
			newMask = imageMask;
		}
		else {
			newMask = increasedMask;
		}
		return newMask;
	}


    private BufferedImage increaseImageWidth() {
        logger.log("Starting Width Increase");
        System.out.println("Starting Width Increase");
        BufferedImage image = newEmptyOutputSizedImage();
		BufferedImage theOriginalImage = newEmptyInputSizedImage();
		increasedMask = new boolean[outHeight][outWidth];
		theOriginalImage = workingImage;


        int numberOfSeams = outWidth - inWidth;
        for (int k = 0; k < numberOfSeams; k++){
            parent = new int[inHeight][inWidth-k];
            cost = new long[inHeight][inWidth-k];
            createEnergyMatrix();
            calculateCostMatrix(k);
            findPath(k);
            removeSeam(k);
        }

        int nextSaveIndex;
        int seamsMappingLength = seamsMapping[0].length;
        for (int y = 0; y < inHeight; y++) {
            int currentX = 0;
            int mappingIndex = 0;
            for (int x = 0; x < inWidth; x++) {
                if (mappingIndex < seamsMappingLength) {
                    nextSaveIndex =  seamsMapping[y][mappingIndex];
                }
                else {
                    nextSaveIndex = Integer.MAX_VALUE;
                }
                int pixelColor = theOriginalImage.getRGB(x, y);
                image.setRGB(currentX, y, pixelColor);
                increasedMask[y][currentX] = imageMask[y][x];
                currentX++;
                if (x < nextSaveIndex) {
                    image.setRGB(currentX, y, pixelColor);
					increasedMask[y][currentX] = imageMask[y][x];
                    currentX++;

                }
                else {
                    mappingIndex++;
                }
            }
        }
		return image;
    }




}
