#pragma version(1)
#pragma rs java_package_name(mobi.daogu.gaussblurstrict.rsc)

int32_t radiusX2;
int32_t pixn;
uchar4 *gDst;
const uchar4 *gSrc;
float *gGuass;

/*__________________________________________
  |      |      |      |      |      |      |
  | c_in | .... |      |      |......|      |
  |______|______|______|______|______|______|
   \___________/
         V      ____________________________
       radius   |      |      |      |      |
                | c_rt |      |......|      |
                |______|______|______|______|
*/

void root(const uint16_t *eachline) {
    uint16_t i = *eachline, size = radiusX2 + 1;
    uint32_t c_in, c_rt, end;
    c_in = i * pixn, c_rt = i * (pixn - radiusX2);
    end = c_rt + pixn - radiusX2;
    float4 color_in, color;
    for (; c_rt < end; c_rt++, c_in++, color = 0.0f) {
        for (i = 0; i < size; i++) {
            color_in = rsUnpackColor8888(gSrc[c_in + i]);
            color += color_in * gGuass[i];
        }
        gDst[c_rt] = rsPackColorTo8888(color);
    }
}