// ================================================================
// Copyright (c) Meta Platforms, Inc. and affiliates.
// ================================================================

package pdqhashing.tools;

import java.awt.image.BufferedImage;

import org.bukkit.Bukkit;

import pdqhashing.hasher.PDQHasher;
import pdqhashing.types.Hash256;
import pdqhashing.types.HashAndQuality;
import pdqhashing.utils.MatrixUtil;

/**
 * Ops/demo tool for computing PDQ hashes of image files (JPEG, PNG, etc.)
 */
public class JustGiveMeThePDQ {
	public static String execute(BufferedImage img) {
		PDQHasher pdqHasher = new PDQHasher();
		int numRows = img.getHeight();
		int numCols = img.getWidth();
		float[] buffer1 = MatrixUtil.allocateMatrixAsRowMajorArray(numRows, numCols);
		float[] buffer2 = MatrixUtil.allocateMatrixAsRowMajorArray(numRows, numCols);
		float[][] buffer64x64 = MatrixUtil.allocateMatrix(64, 64);
		float[][] buffer16x64 = MatrixUtil.allocateMatrix(16, 64);
		float[][] buffer16x16 = MatrixUtil.allocateMatrix(16, 16);
		HashAndQuality rv = pdqHasher.fromBufferedImage(img, buffer1, buffer2, buffer64x64, buffer16x64, buffer16x16);
		Hash256 hash = rv.getHash();
		Bukkit.getServer().getConsoleSender().sendMessage("PDQ hash of image:" + hash.toString());
		return hash.toString();
	}
}
