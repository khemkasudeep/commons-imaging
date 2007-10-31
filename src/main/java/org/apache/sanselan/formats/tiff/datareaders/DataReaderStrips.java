/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sanselan.formats.tiff.datareaders;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.common.BitInputStream;
import org.apache.sanselan.common.ByteSource;
import org.apache.sanselan.formats.tiff.photometricinterpreters.PhotometricInterpreter;

public class DataReaderStrips extends DataReader
{

	private final int bitsPerPixel;

	private final int width, height;

	private final int stripOffsets[];
	private final int stripByteCounts[];
	private final int compression;
	private final int rowsPerStrip;

	public DataReaderStrips(PhotometricInterpreter fPhotometricInterpreter,
			int fBitsPerPixel, int fBitsPerSample[], int Predictor,
			int fSamplesPerPixel, int width, int height, int fStripOffsets[],
			int fStripByteCounts[], int fCompression, int fRowsPerStrip,
			int byteOrder)
	{
		super(fPhotometricInterpreter, fBitsPerSample, Predictor,
				fSamplesPerPixel, byteOrder);

		this.bitsPerPixel = fBitsPerPixel;
		this.width = width;
		this.height = height;
		this.stripOffsets = fStripOffsets;
		this.stripByteCounts = fStripByteCounts;
		this.compression = fCompression;
		this.rowsPerStrip = fRowsPerStrip;

	}

	public void interpretStrip(BufferedImage bi, byte bytes[],
			int pixels_per_strip) throws ImageReadException, IOException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		BitInputStream bis = new BitInputStream(bais);

		for (int i = 0; i < pixels_per_strip; i++)
		{
			int samples[] = getSamplesAsBytes(bis);

			if ((x < width) && (y < height))
			{
				samples = applyPredictor(samples, x);

				photometricInterpreter.interpretPixel(bi, samples, x, y);
			}

			x++;
			if (x >= width)
			{
				x = 0;
				y++;
				bis.flushCache();
				if (y >= height)
					break;
			}
		}
	}

	int x = 0, y = 0;

	public void readImageData(BufferedImage bi, ByteSource byteSource)
			throws ImageReadException, IOException
	{

		for (int strip = 0; strip < stripOffsets.length; strip++)
		{
			int rows_remaining = height - (strip * rowsPerStrip);
			int rows_in_this_strip = Math.min(rows_remaining, rowsPerStrip);
			int pixels_per_strip = rows_in_this_strip * width;
			int bytes_per_strip = ((pixels_per_strip * bitsPerPixel) + 7) / 8;

			int fStripOffset = stripOffsets[strip];
			int fStripByteCount = stripByteCounts[strip];

			byte compressed[] = byteSource.getBlock(fStripOffset,
					fStripByteCount);

			byte decompressed[] = decompress(compressed, compression,
					bytes_per_strip);

			interpretStrip(bi, decompressed, pixels_per_strip);

		}
	}

}