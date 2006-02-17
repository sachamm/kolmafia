/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which retrieves all the adventures currently
 * available to <code>KoLmafia</code>.
 */

public class EquipmentDatabase extends KoLDatabase
{
	private static int [] power = new int[ TradeableItemDatabase.ITEM_COUNT ];
	private static int [] hands = new int[ TradeableItemDatabase.ITEM_COUNT ];
	private static String [] requirement = new String[ TradeableItemDatabase.ITEM_COUNT ];

	static
	{
		BufferedReader reader = getReader( "equipment.dat" );

		String [] data;
		int itemID;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				itemID = TradeableItemDatabase.getItemID( data[0] );

				if ( itemID != -1 )
				{
					power[ itemID ] = Integer.parseInt( data[1] );
					requirement[ itemID ] = data[2];
					hands[ itemID ] = ( data.length >= 4 ) ? Integer.parseInt( data[3] ) :
						TradeableItemDatabase.getConsumptionType( itemID ) == ConsumeItemRequest.EQUIP_WEAPON ? 1 : 0;
				}
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	public static final int OUTFIT_COUNT = 30;
	private static SpecialOutfit [] outfits = new SpecialOutfit[ OUTFIT_COUNT ];
	private static AdventureResult [][] outfitPieces = new AdventureResult[ OUTFIT_COUNT ][];

	static
	{
		BufferedReader reader = getReader( "outfits.dat" );

		String [] data;
		int outfitID;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 3 )
			{
				outfitID = Integer.parseInt( data[0] );
				outfits[ outfitID ] = new SpecialOutfit( outfitID, data[1] );

				String [] pieces = data[2].split( "\\s*,\\s*" );
				outfitPieces[ outfitID ] = new AdventureResult[ pieces.length ];

				for ( int i = 0; i < pieces.length; ++i )
					outfitPieces[ outfitID ][i] = new AdventureResult( pieces[i], 1 );
			}
		}
	}

	public static boolean contains( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );
		return itemID != -1 && requirement[ itemID ] != null;
	}

	public static boolean canEquip( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 || requirement[ itemID ] == null )
			return false;

		if ( requirement[ itemID ].startsWith( "Mus:" ) )
			return KoLCharacter.getBaseMuscle() >= Integer.parseInt( requirement[ itemID ].substring(5) );

		if ( requirement[ itemID ].startsWith( "Mys:" ) )
			return KoLCharacter.getBaseMysticality() >= Integer.parseInt( requirement[ itemID ].substring(5) );

		if ( requirement[ itemID ].startsWith( "Mox:" ) )
			return KoLCharacter.getBaseMoxie() >= Integer.parseInt( requirement[ itemID ].substring(5) );

		return true;
	}

	public static int getPower( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return power[ itemID ];
	}

	public static int getHands( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return hands[ itemID ];
	}

	public static boolean hasOutfit( int id )
	{	return KoLCharacter.getOutfits().contains( outfits[id] );
	}

	public static SpecialOutfit getOutfit( int id )
	{	return outfits[id];
	}

	public static void updateOutfits()
	{
		LockableListModel available = KoLCharacter.getOutfits();

		for ( int i = 0; i < OUTFIT_COUNT; ++i )
		{
			if ( outfits[i] != null )
			{
				boolean hasAllPieces = true;
				for ( int j = 0; j < outfitPieces[i].length; ++j )
					hasAllPieces &= KoLCharacter.hasItem( outfitPieces[i][j], true );

				// If the player has all the pieces, but it's not on the
				// list, then add it to the list of available outfits.

				if ( hasAllPieces && !available.contains( outfits[i] ) )
					available.add( outfits[i] );

				// If the player does not have all the pieces, but it is
				// on the list, then remove it from the list.

				if ( !hasAllPieces && available.contains( outfits[i] ) )
					available.remove( outfits[i] );
			}
		}
	}

	/**
	 * Utility method which determines whether or not the equipment
	 * corresponding to the given outfit is already equipped.
	 */

	public static boolean isWearingOutfit( int outfitID )
	{
		boolean isWearingOutfit = true;
		for ( int i = 0; i < outfitPieces[ outfitID ].length; ++i )
		{
			switch ( TradeableItemDatabase.getConsumptionType( outfitPieces[ outfitID ][i].getName() ) )
			{
				case ConsumeItemRequest.EQUIP_WEAPON:
					isWearingOutfit &= KoLCharacter.getEquipment( KoLCharacter.WEAPON ).equals( outfitPieces[ outfitID ][i] );
					break;

				case ConsumeItemRequest.EQUIP_OFFHAND:
					isWearingOutfit &= KoLCharacter.getEquipment( KoLCharacter.OFFHAND ).equals( outfitPieces[ outfitID ][i] );
					break;

				case ConsumeItemRequest.EQUIP_HAT:
					isWearingOutfit &= KoLCharacter.getEquipment( KoLCharacter.HAT ).equals( outfitPieces[ outfitID ][i] );
					break;

				case ConsumeItemRequest.EQUIP_SHIRT:
					isWearingOutfit &= KoLCharacter.getEquipment( KoLCharacter.SHIRT ).equals( outfitPieces[ outfitID ][i] );
					break;

				case ConsumeItemRequest.EQUIP_PANTS:
					isWearingOutfit &= KoLCharacter.getEquipment( KoLCharacter.PANTS ).equals( outfitPieces[ outfitID ][i] );
					break;

				case ConsumeItemRequest.EQUIP_ACCESSORY:

					isWearingOutfit &=
						KoLCharacter.getEquipment( KoLCharacter.ACCESSORY1 ).equals( outfitPieces[ outfitID ][i] ) ||
						KoLCharacter.getEquipment( KoLCharacter.ACCESSORY2 ).equals( outfitPieces[ outfitID ][i] ) ||
						KoLCharacter.getEquipment( KoLCharacter.ACCESSORY3 ).equals( outfitPieces[ outfitID ][i] );

					break;
			}
		}

		return isWearingOutfit;
	}

	public static boolean addOutfitConditions( KoLAdventure adventure )
	{
		LockableListModel conditions = client.getConditions();
		String adventureID = adventure.getAdventureID();

		if ( adventureID.equals( "41" ) )
		{
			addOutfitConditions( 5 );
			return true;
		}

		if ( adventureID.equals( "42" ) )
		{
			addOutfitConditions( 4 );
			return true;
		}

		if ( adventureID.equals( "61" ) )
		{
			addOutfitConditions( 8 );
			return true;
		}

		if ( adventureID.equals( "63" ) )
		{
			addOutfitConditions( 7 );
			return true;
		}

		if ( adventureID.equals( "26" ) )
		{
			addOutfitConditions( 2 );
			return true;
		}

		if ( adventureID.equals( "27" ) )
		{
			addOutfitConditions( 3 );
			return true;
		}

		if ( adventureID.equals( "66" ) )
		{
			addOutfitConditions( 9 );
			return true;
		}

		if ( adventureID.equals( "85" ) )
		{
			addOutfitConditions( 23 );
			return true;
		}

		// No outfit existed for this are; therefore,
		// return false.

		return false;
	}

	public static void addOutfitConditions( int outfitID )
	{
		// Ignore custom outfits, since there's
		// no way to know what they are (yet).

		if ( outfitID < 1 )
			return;

		for ( int i = 0; i < outfitPieces[ outfitID ].length; ++i )
			DEFAULT_SHELL.executeConditionsCommand( "add " + outfitPieces[ outfitID ][i].getName() );
	}
}
