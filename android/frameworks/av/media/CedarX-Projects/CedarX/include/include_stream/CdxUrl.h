#ifndef CDX_URL
#define CDX_URL

typedef struct AW_URL
{
    char* url;
    char* protocol;
    char* hostname;
    char* file;
    unsigned int port;
    char* username;
    char* password;
    char* noauth_url;
}CdxUrlT;

void CdxUrlFree(CdxUrlT* curUrl);

void CdxUrlEscapeString(char *outbuf, const char *inbuf);

void CdxUrlUnescapeString(char *outbuf, const char *inbuf);

void CdxUrlEscapeStringPart(char *outbuf, const char *inbuf);

#endif
