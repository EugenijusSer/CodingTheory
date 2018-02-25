import java.util.Random;

public class Engine {

    //uzkoduoja vartotojo ivesti (ima bitu masyva ir grazina uzkoduotus bitus)
    public int[] encode (int input[]){

        int bits[] = new int[input.length + 6]; //prie vartotojo ivesties yra pridedami dar 6 nuliniai bitai
        System.arraycopy( input, 0, bits, 0, input.length );
        int memory[] = {0,0,0,0,0,0}; //atminties registrai
        int encodedBits[] = new int[bits.length * 2]; //uzkoduotu bitu masyvo dydis yra dvigubai didesnis uz pradini
        int tempArray[] = new int[6]; //laikinas atminties registru masyvas

        for(int i = 0; i < bits.length; i++){
            System.arraycopy( memory, 0, tempArray, 0, memory.length ); //atminties registru reiksmes yra nukopijuojamos i laikina masyva
            //pirmas bitas eina iskarto i uzkoduotu bitu masyva
            encodedBits[i*2] = bits[i];
            //antras bitas yra apskaiciuojamas sudedant pirmaji bita su atitinkamais atminties registrais ir atliekama modulio operacija
            encodedBits[i*2+1] = (bits[i] + memory[1] + memory [4] + memory[5]) % 2;

            //stumiam registrus i desine
            System.arraycopy(tempArray,0,memory,1,5);
            memory[0] = bits[i];
        }
        return encodedBits;
    }

    //siuncia vartotojo ivesti kanalu (ima bitu masyva bei klaidos tikimybe ir grazina bitu seka praejusia kanala)
    public int[] sendThroughNoisyChannel (int bits[], double probability){

        int channelBits[] = new int[bits.length]; //pro kanala praejes bitu masyvas bus tokio pat ilgio
        System.arraycopy(bits,0,channelBits,0,bits.length);
        for (int i = 0; i < channelBits.length; i++){
            Random rand = new Random();
            //traukiamas atsitiktinis skaicius tarp 0 ir 1 ir ziurima ar jis mazesnis uz tikimybe, jei taip, bitas yra apverciamas
            if(rand.nextDouble() < probability){
                channelBits[i] = (channelBits[i] + 1) % 2;
            }
        }
        return channelBits;
    }

    //dekoduoja vartotojo ivesti (ima bitu masyva ir grazina dekoduota bitu seka)
    public int[] decode (int encodedBits[]){

        int mde;
        int upperMemory[] = {0,0,0,0,0,0}; //virsutinieji atminties registrai
        int lowerMemory[] = {0,0,0,0,0,0}; //apatiniai atminties registrai
        int tempUpperMemory[] = new int[6];
        int tempLowerMemory[] = new int[6];
        int decodedBits[] = new int[encodedBits.length / 2];
        for(int i = 0; i < decodedBits.length; i++){
            System.arraycopy( upperMemory, 0, tempUpperMemory, 0, upperMemory.length );
            System.arraycopy( lowerMemory, 0, tempLowerMemory, 0, lowerMemory.length );
            //apskaiciuojama pirmoji modulus operacija
            int firstPlus = (encodedBits[i * 2] + encodedBits[i * 2 + 1] + upperMemory[1] + upperMemory[4] + upperMemory[5]) % 2;

            //apskaiciuoja mde
            if((firstPlus + lowerMemory[0] + lowerMemory[3] + lowerMemory[5]) > 2)
                mde = 1;
            else
                mde = 0;

            //apskaiciuojamas dekoduotas bitas sudedant paskutinio virsutinio registro bita su sindromo bitu
            decodedBits[i] = (upperMemory[5] + mde) % 2;

            //stumia virsutinius registrus i desine
            System.arraycopy(tempUpperMemory,0,upperMemory,1,5);
            upperMemory[0] = encodedBits[i*2];

            //stumia apatinius registrus i desine (atitinkamai kartu atlieka veiksmus tose vietosu kur to reikalauja algoritmas)
            lowerMemory[0] = (firstPlus + mde) % 2;
            lowerMemory[1] = (tempLowerMemory[0] + mde) % 2;
            lowerMemory[2] = tempLowerMemory[1];
            lowerMemory[3] = tempLowerMemory[2];
            lowerMemory[4] = (tempLowerMemory[3] + mde) % 2;
            lowerMemory[5] = tempLowerMemory[4];
        }

        //is dekoduotos sekos yra pasalinami pirmieji 6 bitai, nes jie neturi reiksmes
        int correctDecodedBits[] = new int[decodedBits.length - 6];
        System.arraycopy( decodedBits, 6, correctDecodedBits, 0, correctDecodedBits.length);
        return correctDecodedBits;
    }
}
