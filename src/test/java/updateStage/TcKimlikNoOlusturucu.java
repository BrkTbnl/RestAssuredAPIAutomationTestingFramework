package updateStage;

import java.util.Random;

public class TcKimlikNoOlusturucu {

    public static void main(String[] args) {
        System.out.println(tcKimlikNoOlustur());
    }

    public static String tcKimlikNoOlustur() {
        Random random = new Random();
        int[] tcNo = new int[11];

        tcNo[0] = random.nextInt(9) + 1; // İlk rakam 0 olamaz
        for (int i = 1; i < 9; i++) {
            tcNo[i] = random.nextInt(10);
        }
        int oddSum = tcNo[0] + tcNo[2] + tcNo[4] + tcNo[6] + tcNo[8];
        int evenSum = tcNo[1] + tcNo[3] + tcNo[5] + tcNo[7];
        tcNo[9] = (7 * oddSum + 9 * evenSum) % 10;

        tcNo[10] = (8 * oddSum) % 10;
        if ((oddSum + evenSum + tcNo[9]) % 10 != tcNo[10]) {
            return tcKimlikNoOlustur();
        }
        StringBuilder sb = new StringBuilder();
        for (int num : tcNo) {
            sb.append(num);
        }
        return sb.toString();
    }
}