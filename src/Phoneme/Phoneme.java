/**
 * DOCKS is a framework for post-processing results of Cloud-based speech 
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact:
 * 7twiefel@informatik.uni-hamburg.de
 */
package Phoneme;

import Phoneme.Categories.Excitation;
import Phoneme.Categories.MannerOfArticulation;
import Phoneme.Categories.PlaceOfArticfulation;
import Phoneme.Categories.VowelBackness;
import Phoneme.Categories.VowelHeight;


/**
 * Phoneme class used to calculate the confusion costs
 * contains getters and setters for the different categories derived from IPA table.
 * Self explaining. See package Phoneme.Categories for the different category values.
 * @author 7twiefel
 *
 */
public class Phoneme {
	
	


	private String phoneme;
	private Excitation pronounciation = Excitation.VOICED;
	private PlaceOfArticfulation phonemeClass;
	private MannerOfArticulation articulation = MannerOfArticulation.UNKNOWN;
	private VowelBackness vowelArticulation;
	private VowelHeight mouthStatus;
	private VowelBackness vowelArticulation2;
	private VowelHeight mouthStatus2;
	public VowelBackness getVowelArticulation2() {
		return vowelArticulation2;
	}
	public String toString()
	{
		return phoneme;
	}
	public void setVowelArticulation2(VowelBackness vowelArticulation2) {
		this.vowelArticulation2 = vowelArticulation2;
	}

	public VowelHeight getMouthStatus2() {
		return mouthStatus2;
	}

	public void setMouthStatus2(VowelHeight mouthStatus2) {
		this.mouthStatus2 = mouthStatus2;
	}

	public Phoneme(String phoneme, PlaceOfArticfulation phonemeClass,
			VowelBackness vowelArticulation, VowelHeight mouthStatus,
			VowelBackness vowelArticulation2, VowelHeight mouthStatus2) {
		super();
		this.phoneme = phoneme;
		this.phonemeClass = phonemeClass;
		this.vowelArticulation = vowelArticulation;
		this.mouthStatus = mouthStatus;
		this.vowelArticulation2 = vowelArticulation2;
		this.mouthStatus2 = mouthStatus2;
	}

	public Excitation getPronounciation() {
		return pronounciation;
	}

	public void setPronounciation(Excitation pronounciation) {
		this.pronounciation = pronounciation;
	}

	public MannerOfArticulation getArticulation() {
		return articulation;
	}

	public void setArticulation(MannerOfArticulation articulation) {
		this.articulation = articulation;
	}

	public VowelBackness getVowelArticulation() {
		return vowelArticulation;
	}

	public void setVowelArticulation(VowelBackness vowelArticulation) {
		this.vowelArticulation = vowelArticulation;
	}

	public VowelHeight getMouthStatus() {
		return mouthStatus;
	}

	public void setMouthStatus(VowelHeight mouthStatus) {
		this.mouthStatus = mouthStatus;
	}

	public Phoneme(String phoneme, 	PlaceOfArticfulation phonemeClass, VowelBackness vowelArticulation,
			VowelHeight mouthStatus) {
		super();
		this.phoneme = phoneme;
		this.phonemeClass = phonemeClass;
		this.vowelArticulation = vowelArticulation;
		this.mouthStatus = mouthStatus;
	}
	
	

	public Phoneme(String phoneme, Excitation pronounciation,
			PlaceOfArticfulation phonemeClass, MannerOfArticulation articulation) {
		super();
		this.phoneme = phoneme;
		this.pronounciation = pronounciation;
		this.phonemeClass = phonemeClass;
		this.articulation = articulation;
	}
	
	public Phoneme(String phoneme, Excitation pronounciation,
			PlaceOfArticfulation phonemeClass) {
		super();
		this.phoneme = phoneme;
		this.pronounciation = pronounciation;
		this.phonemeClass = phonemeClass;

	}

	public String getPhoneme() {
		return phoneme;
	}
	public void setPhoneme(String phoneme) {
		this.phoneme = phoneme;
	}
	public Excitation getpronounciation() {
		return pronounciation;
	}
	public void setpronounciation(Excitation pronounciation) {
		this.pronounciation = pronounciation;
	}
	public PlaceOfArticfulation getPhonemeClass() {
		return phonemeClass;
	}
	public void setPhonemeClass(PlaceOfArticfulation phonemeClass) {
		this.phonemeClass = phonemeClass;
	}

}
