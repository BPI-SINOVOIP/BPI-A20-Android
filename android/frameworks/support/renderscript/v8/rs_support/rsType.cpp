/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "rsContext.h"

using namespace android;
using namespace android::renderscript;

Type::Type(Context *rsc) : ObjectBase(rsc) {
    memset(&mHal, 0, sizeof(mHal));
    mDimLOD = false;
}

void Type::preDestroy() const {
    for (uint32_t ct = 0; ct < mRSC->mStateType.mTypes.size(); ct++) {
        if (mRSC->mStateType.mTypes[ct] == this) {
            mRSC->mStateType.mTypes.removeAt(ct);
            break;
        }
    }
}

Type::~Type() {
    clear();
}

void Type::clear() {
    if (mHal.state.lodCount) {
        delete [] mHal.state.lodDimX;
        delete [] mHal.state.lodDimY;
        delete [] mHal.state.lodDimZ;
        delete [] mHal.state.lodOffset;
    }
    mElement.clear();
    memset(&mHal, 0, sizeof(mHal));
}

TypeState::TypeState() {
}

TypeState::~TypeState() {
    rsAssert(!mTypes.size());
}

size_t Type::getOffsetForFace(uint32_t face) const {
    rsAssert(mHal.state.faces);
    return 0;
}

void Type::compute() {
    uint32_t oldLODCount = mHal.state.lodCount;
    if (mDimLOD) {
        uint32_t l2x = rsFindHighBit(mHal.state.dimX) + 1;
        uint32_t l2y = rsFindHighBit(mHal.state.dimY) + 1;
        uint32_t l2z = rsFindHighBit(mHal.state.dimZ) + 1;

        mHal.state.lodCount = rsMax(l2x, l2y);
        mHal.state.lodCount = rsMax(mHal.state.lodCount, l2z);
    } else {
        mHal.state.lodCount = 1;
    }
    if (mHal.state.lodCount != oldLODCount) {
        if (oldLODCount) {
            delete [] mHal.state.lodDimX;
            delete [] mHal.state.lodDimY;
            delete [] mHal.state.lodDimZ;
            delete [] mHal.state.lodOffset;
        }
        mHal.state.lodDimX = new uint32_t[mHal.state.lodCount];
        mHal.state.lodDimY = new uint32_t[mHal.state.lodCount];
        mHal.state.lodDimZ = new uint32_t[mHal.state.lodCount];
        mHal.state.lodOffset = new uint32_t[mHal.state.lodCount];
    }

    uint32_t tx = mHal.state.dimX;
    uint32_t ty = mHal.state.dimY;
    uint32_t tz = mHal.state.dimZ;
    size_t offset = 0;
    for (uint32_t lod=0; lod < mHal.state.lodCount; lod++) {
        mHal.state.lodDimX[lod] = tx;
        mHal.state.lodDimY[lod] = ty;
        mHal.state.lodDimZ[lod]  = tz;
        mHal.state.lodOffset[lod] = offset;
        offset += tx * rsMax(ty, 1u) * rsMax(tz, 1u) * mElement->getSizeBytes();
        if (tx > 1) tx >>= 1;
        if (ty > 1) ty >>= 1;
        if (tz > 1) tz >>= 1;
    }

    // At this point the offset is the size of a mipmap chain;
    mMipChainSizeBytes = offset;

    if (mHal.state.faces) {
        offset *= 6;
    }
    mTotalSizeBytes = offset;
    mHal.state.element = mElement.get();
}

uint32_t Type::getLODOffset(uint32_t lod, uint32_t x) const {
    uint32_t offset = mHal.state.lodOffset[lod];
    offset += x * mElement->getSizeBytes();
    return offset;
}

uint32_t Type::getLODOffset(uint32_t lod, uint32_t x, uint32_t y) const {
    uint32_t offset = mHal.state.lodOffset[lod];
    offset += (x + y * mHal.state.lodDimX[lod]) * mElement->getSizeBytes();
    return offset;
}

uint32_t Type::getLODOffset(uint32_t lod, uint32_t x, uint32_t y, uint32_t z) const {
    uint32_t offset = mHal.state.lodOffset[lod];
    offset += (x +
               y * mHal.state.lodDimX[lod] +
               z * mHal.state.lodDimX[lod] * mHal.state.lodDimY[lod]) * mElement->getSizeBytes();
    return offset;
}

uint32_t Type::getLODFaceOffset(uint32_t lod, RsAllocationCubemapFace face,
                                uint32_t x, uint32_t y) const {
    uint32_t offset = mHal.state.lodOffset[lod];
    offset += (x + y * mHal.state.lodDimX[lod]) * mElement->getSizeBytes();

    if (face != 0) {
        uint32_t faceOffset = getSizeBytes() / 6;
        offset += faceOffset * face;
    }
    return offset;
}

void Type::dumpLOGV(const char *prefix) const {
    char buf[1024];
    ObjectBase::dumpLOGV(prefix);
    ALOGV("%s   Type: x=%u y=%u z=%u mip=%i face=%i", prefix,
                                                      mHal.state.dimX,
                                                      mHal.state.dimY,
                                                      mHal.state.dimZ,
                                                      mHal.state.lodCount,
                                                      mHal.state.faces);
    snprintf(buf, sizeof(buf), "%s element: ", prefix);
    mElement->dumpLOGV(buf);
}

void Type::serialize(Context *rsc, OStream *stream) const {
    // Need to identify ourselves
    stream->addU32((uint32_t)getClassId());

    String8 name(getName());
    stream->addString(&name);

    mElement->serialize(rsc, stream);

    stream->addU32(mHal.state.dimX);
    stream->addU32(mHal.state.dimY);
    stream->addU32(mHal.state.dimZ);

    stream->addU8((uint8_t)(mHal.state.lodCount ? 1 : 0));
    stream->addU8((uint8_t)(mHal.state.faces ? 1 : 0));
}

Type *Type::createFromStream(Context *rsc, IStream *stream) {
    // First make sure we are reading the correct object
    RsA3DClassID classID = (RsA3DClassID)stream->loadU32();
    if (classID != RS_A3D_CLASS_ID_TYPE) {
        ALOGE("type loading skipped due to invalid class id\n");
        return NULL;
    }

    String8 name;
    stream->loadString(&name);

    Element *elem = Element::createFromStream(rsc, stream);
    if (!elem) {
        return NULL;
    }

    uint32_t x = stream->loadU32();
    uint32_t y = stream->loadU32();
    uint32_t z = stream->loadU32();
    uint8_t lod = stream->loadU8();
    uint8_t faces = stream->loadU8();
    Type *type = Type::getType(rsc, elem, x, y, z, lod != 0, faces !=0 );
    elem->decUserRef();
    return type;
}

bool Type::getIsNp2() const {
    uint32_t x = getDimX();
    uint32_t y = getDimY();
    uint32_t z = getDimZ();

    if (x && (x & (x-1))) {
        return true;
    }
    if (y && (y & (y-1))) {
        return true;
    }
    if (z && (z & (z-1))) {
        return true;
    }
    return false;
}

ObjectBaseRef<Type> Type::getTypeRef(Context *rsc, const Element *e,
                                     uint32_t dimX, uint32_t dimY, uint32_t dimZ,
                                     bool dimLOD, bool dimFaces) {
    ObjectBaseRef<Type> returnRef;

    TypeState * stc = &rsc->mStateType;

    ObjectBase::asyncLock();
    for (uint32_t ct=0; ct < stc->mTypes.size(); ct++) {
        Type *t = stc->mTypes[ct];
        if (t->getElement() != e) continue;
        if (t->getDimX() != dimX) continue;
        if (t->getDimY() != dimY) continue;
        if (t->getDimZ() != dimZ) continue;
        if (t->getDimLOD() != dimLOD) continue;
        if (t->getDimFaces() != dimFaces) continue;
        returnRef.set(t);
        ObjectBase::asyncUnlock();
        return returnRef;
    }
    ObjectBase::asyncUnlock();


    Type *nt = new Type(rsc);
    nt->mDimLOD = dimLOD;
    returnRef.set(nt);
    nt->mElement.set(e);
    nt->mHal.state.dimX = dimX;
    nt->mHal.state.dimY = dimY;
    nt->mHal.state.dimZ = dimZ;
    nt->mHal.state.faces = dimFaces;
    nt->compute();

    ObjectBase::asyncLock();
    stc->mTypes.push(nt);
    ObjectBase::asyncUnlock();

    return returnRef;
}

ObjectBaseRef<Type> Type::cloneAndResize1D(Context *rsc, uint32_t dimX) const {
    return getTypeRef(rsc, mElement.get(), dimX,
                      getDimY(), getDimZ(), getDimLOD(), getDimFaces());
}

ObjectBaseRef<Type> Type::cloneAndResize2D(Context *rsc,
                              uint32_t dimX,
                              uint32_t dimY) const {
    return getTypeRef(rsc, mElement.get(), dimX, dimY,
                      getDimZ(), getDimLOD(), getDimFaces());
}


void Type::incRefs(const void *ptr, size_t ct, size_t startOff) const {
    const uint8_t *p = static_cast<const uint8_t *>(ptr);
    const Element *e = mHal.state.element;
    uint32_t stride = e->getSizeBytes();

    p += stride * startOff;
    while (ct > 0) {
        e->incRefs(p);
        ct--;
        p += stride;
    }
}


void Type::decRefs(const void *ptr, size_t ct, size_t startOff) const {
    if (!mHal.state.element->getHasReferences()) {
        return;
    }
    const uint8_t *p = static_cast<const uint8_t *>(ptr);
    const Element *e = mHal.state.element;
    uint32_t stride = e->getSizeBytes();

    p += stride * startOff;
    while (ct > 0) {
        e->decRefs(p);
        ct--;
        p += stride;
    }
}


//////////////////////////////////////////////////
//
namespace android {
namespace renderscript {

RsType rsi_TypeCreate(Context *rsc, RsElement _e, uint32_t dimX,
                     uint32_t dimY, uint32_t dimZ, bool mips, bool faces) {
    Element *e = static_cast<Element *>(_e);

    return Type::getType(rsc, e, dimX, dimY, dimZ, mips, faces);
}

}
}

void rsaTypeGetNativeData(RsContext con, RsType type, uint32_t *typeData, uint32_t typeDataSize) {
    rsAssert(typeDataSize == 6);
    // Pack the data in the follofing way mHal.state.dimX; mHal.state.dimY; mHal.state.dimZ;
    // mHal.state.lodCount; mHal.state.faces; mElement; into typeData
    Type *t = static_cast<Type *>(type);

    (*typeData++) = t->getDimX();
    (*typeData++) = t->getDimY();
    (*typeData++) = t->getDimZ();
    (*typeData++) = t->getDimLOD() ? 1 : 0;
    (*typeData++) = t->getDimFaces() ? 1 : 0;
    (*typeData++) = (uint32_t)t->getElement();
    t->getElement()->incUserRef();
}
