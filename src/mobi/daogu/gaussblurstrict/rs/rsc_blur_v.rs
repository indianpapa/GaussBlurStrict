#pragma version(1)
#pragma rs java_package_name(mobi.daogu.gaussblurstrict.rsc)

int32_t radiusX2;
int32_t pixn;
int32_t width;
uchar4 *gDst;
const uchar4 *gSrc;
float *gGuass;

/*________
  |      |\
  | c_in | |
  |______|  => radius
  |      | |
  | .... | |
  |______|/ ________
  |      |  |      |
  |      |  | c_rt |
  |______|  |______|
  |      |  |      |
  |      |  |      |
  |______|  |______|
  |      |  |      |
  | .... |  | .... |
  |______|  |______|
  |      |  |      |
  |      |  |      |
  |______|  |______|
*/

void root(const uint16_t *eachline) {
    uint16_t i, j, size;
    uint32_t end, c_in, c_rt, in_tp;
    float4 color_in, color;
    size = radiusX2 + 1;
    end = pixn - radiusX2;
    c_in = c_rt = *eachline;

    for (i = 0; i < end; i++, c_in += width, c_rt += width, color = 0.0f) {
        in_tp = c_in;
        for (j = 0; j < size; j++, in_tp += width) {
            color_in = rsUnpackColor8888(gSrc[in_tp]);
            color += color_in * gGuass[j];
        }
        gDst[c_rt] = rsPackColorTo8888(color);
    }
}