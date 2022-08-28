/*-
 * #%L
 * Commands and function for opening, conversion and easy use of bioformats format into BigDataViewer
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, BIOP, EPFL
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the BIOP nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package ch.epfl.biop.bdv.bioformats.imageloader;

import mpicbg.spim.data.generic.base.NamedEntity;

public class SeriesNumber extends NamedEntity implements
	Comparable<SeriesNumber>
{

	public SeriesNumber(final int id, final String name) {
		super(id, name);
	}

	public SeriesNumber(final int id) {
		this(id, Integer.toString(id));
	}

	/**
	 * Get the unique id of this location.
	 */
	@Override
	public int getId() {
		return super.getId();
	}

	/**
	 * Get the name of this tile. The name is used for example to replace it in
	 * filenames when opening individual 3d-stacks (e.g.
	 * SPIM_TL20_Tile1_Angle45.tif)
	 */
	@Override
	public String getName() {
		return super.getName();
	}

	/**
	 * Set the name of this tile.
	 */
	@Override
	public void setName(final String name) {
		super.setName(name);
	}

	/**
	 * Compares the {@link #getId() ids}.
	 */
	@Override
	public int compareTo(final SeriesNumber o) {
		return getId() - o.getId();
	}

	protected SeriesNumber() {}
}
