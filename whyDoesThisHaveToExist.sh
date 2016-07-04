function json
{
    f=$(basename "$1")
    f=${f%.*}
    echo "{\"parent\": \"item/handheld\",\"textures\": {\"layer0\": \"lumberjack:items/${f}\"}}" > src/main/resources/assets/lumberjack/models/item/${f}.json
}


for f in src/main/resources/assets/lumberjack/textures/items/*.png; do json "${f%.*}"; done;
