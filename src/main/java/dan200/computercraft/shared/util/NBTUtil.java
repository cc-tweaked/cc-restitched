/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.common.io.BaseEncoding;
import dan200.computercraft.ComputerCraft;
import net.minecraft.nbt.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public final class NBTUtil
{
    private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();

    private NBTUtil() {}

    private static NbtElement toNBTTag( Object object )
    {
        if( object == null ) return null;
        if( object instanceof Boolean ) return NbtByte.of( (byte) ((boolean) (Boolean) object ? 1 : 0) );
        if( object instanceof Number ) return NbtDouble.of( ((Number) object).doubleValue() );
        if( object instanceof String ) return NbtString.of( object.toString() );
        if( object instanceof Map<?, ?> m )
        {
            NbtCompound nbt = new NbtCompound();
            int i = 0;
            for( Map.Entry<?, ?> entry : m.entrySet() )
            {
                NbtElement key = toNBTTag( entry.getKey() );
                NbtElement value = toNBTTag( entry.getKey() );
                if( key != null && value != null )
                {
                    nbt.put( "k" + i, key );
                    nbt.put( "v" + i, value );
                    i++;
                }
            }
            nbt.putInt( "len", m.size() );
            return nbt;
        }

        return null;
    }

    public static NbtCompound encodeObjects( Object[] objects )
    {
        if( objects == null || objects.length <= 0 ) return null;

        NbtCompound nbt = new NbtCompound();
        nbt.putInt( "len", objects.length );
        for( int i = 0; i < objects.length; i++ )
        {
            NbtElement child = toNBTTag( objects[i] );
            if( child != null ) nbt.put( Integer.toString( i ), child );
        }
        return nbt;
    }

    private static Object fromNBTTag( NbtElement tag )
    {
        if( tag == null ) return null;
        switch( tag.getType() )
        {
            case NbtElement.BYTE_TYPE:
                return ((NbtByte) tag).byteValue() > 0;
            case NbtElement.DOUBLE_TYPE:
                return ((NbtDouble) tag).doubleValue();
            default:
            case NbtElement.STRING_TYPE:
                return tag.asString();
            case NbtElement.COMPOUND_TYPE:
            {
                NbtCompound c = (NbtCompound) tag;
                int len = c.getInt( "len" );
                Map<Object, Object> map = new HashMap<>( len );
                for( int i = 0; i < len; i++ )
                {
                    Object key = fromNBTTag( c.get( "k" + i ) );
                    Object value = fromNBTTag( c.get( "v" + i ) );
                    if( key != null && value != null ) map.put( key, value );
                }
                return map;
            }
        }
    }

    public static Object toLua( NbtElement tag )
    {
        if( tag == null ) return null;

        byte typeID = tag.getType();
        switch( typeID )
        {
            case NbtElement.BYTE_TYPE:
            case NbtElement.SHORT_TYPE:
            case NbtElement.INT_TYPE:
            case NbtElement.LONG_TYPE:
                return ((AbstractNbtNumber) tag).longValue();
            case NbtElement.FLOAT_TYPE:
            case NbtElement.DOUBLE_TYPE:
                return ((AbstractNbtNumber) tag).doubleValue();
            case NbtElement.STRING_TYPE: // String
                return tag.asString();
            case NbtElement.COMPOUND_TYPE: // Compound
            {
                NbtCompound compound = (NbtCompound) tag;
                Map<String, Object> map = new HashMap<>( compound.getSize() );
                for( String key : compound.getKeys() )
                {
                    Object value = toLua( compound.get( key ) );
                    if( value != null ) map.put( key, value );
                }
                return map;
            }
            case NbtElement.LIST_TYPE:
            {
                NbtList list = (NbtList) tag;
                Map<Integer, Object> map = new HashMap<>( list.size() );
                for( int i = 0; i < list.size(); i++ ) map.put( i, toLua( list.get( i ) ) );
                return map;
            }
            case NbtElement.BYTE_ARRAY_TYPE:
            {
                byte[] array = ((NbtByteArray) tag).getByteArray();
                Map<Integer, Byte> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }
            case NbtElement.INT_ARRAY_TYPE:
            {
                int[] array = ((NbtIntArray) tag).getIntArray();
                Map<Integer, Integer> map = new HashMap<>( array.length );
                for( int i = 0; i < array.length; i++ ) map.put( i + 1, array[i] );
                return map;
            }

            default:
                return null;
        }
    }

    public static Object[] decodeObjects( NbtCompound tag )
    {
        int len = tag.getInt( "len" );
        if( len <= 0 ) return null;

        Object[] objects = new Object[len];
        for( int i = 0; i < len; i++ )
        {
            String key = Integer.toString( i );
            if( tag.contains( key ) )
            {
                objects[i] = fromNBTTag( tag.get( key ) );
            }
        }
        return objects;
    }

    @Nullable
    public static String getNBTHash( @Nullable NbtCompound tag )
    {
        if( tag == null ) return null;

        try
        {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            DataOutput output = new DataOutputStream( new DigestOutputStream( digest ) );
            NbtIo.write( tag, output );
            byte[] hash = digest.digest();
            return ENCODING.encode( hash );
        }
        catch( NoSuchAlgorithmException | IOException e )
        {
            ComputerCraft.log.error( "Cannot hash NBT", e );
            return null;
        }
    }

    private static final class DigestOutputStream extends OutputStream
    {
        private final MessageDigest digest;

        DigestOutputStream( MessageDigest digest )
        {
            this.digest = digest;
        }

        @Override
        public void write( @Nonnull byte[] b, int off, int len )
        {
            digest.update( b, off, len );
        }

        @Override
        public void write( int b )
        {
            digest.update( (byte) b );
        }
    }
}
