
#include "RenderScript.h"
#include "Element.h"
#include "Type.h"
#include "Allocation.h"

#include "ScriptC_mono.h"

using namespace android;
using namespace renderscriptCpp;

int main(int argc, char** argv)
{

    RenderScript *rs = new RenderScript();
    printf("New RS %p\n", rs);

    bool r = rs->init(16);
    printf("Init returned %i\n", r);

    sp<const Element> e = Element::RGBA_8888(rs);
    printf("Element %p\n", e.get());

    Type::Builder tb(rs, e);
    tb.setX(128);
    tb.setY(128);
    sp<const Type> t = tb.create();
    printf("Type %p\n", t.get());


    sp<Allocation> a1 = Allocation::createSized(rs, e, 1000);
    printf("Allocation %p\n", a1.get());

    sp<Allocation> ain = Allocation::createTyped(rs, t);
    sp<Allocation> aout = Allocation::createTyped(rs, t);
    printf("Allocation %p %p\n", ain.get(), aout.get());

    sp<ScriptC_mono> sc = new ScriptC_mono(rs, NULL, 0);
    printf("new script\n");

    uint32_t *buf = new uint32_t[t->getCount()];
    for (uint32_t ct=0; ct < t->getCount(); ct++) {
        buf[ct] = ct | (ct << 16);
    }
    //ain->copy1DRangeFrom(0, 128*128, (int32_t *)buf, 128*128*4);
    ain->copy1DRangeFromUnchecked(0, t->getCount(), buf, t->getCount()*4);



    sc->forEach_root(ain, aout);
    printf("for each done\n");


    printf("Deleting stuff\n");
    sc.clear();
    t.clear();
    a1.clear();
    e.clear();
    delete rs;
    printf("Delete OK\n");
}
